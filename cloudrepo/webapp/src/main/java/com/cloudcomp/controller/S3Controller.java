package com.cloudcomp.controller;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.cloudcomp.Pojo.Bill;
import com.cloudcomp.Pojo.File;
import com.cloudcomp.Pojo.User;
import com.cloudcomp.Repository.BillRepository;
import com.cloudcomp.Repository.FileRepository;
import com.cloudcomp.Repository.UserRepository;
import com.cloudcomp.util.S3Util;
import com.google.gson.Gson;
import com.timgroup.statsd.StatsDClient;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Profile("aws")
@Controller
public class S3Controller {
    @Autowired
    UserRepository userRepository;

    @Autowired
    BillRepository billRepository;

    @Autowired
    FileRepository fileRepository;
    
    @Autowired
    private Environment environment;

    @Autowired(required = false)
    private StatsDClient stats;

    private final static Logger logger = LoggerFactory.getLogger(S3Controller.class);

    @PostMapping(path = "v1/bill/{id}/file", produces = "application/json", consumes = "multipart/form-data")
    @ResponseBody
    public ResponseEntity<String> addFile(@RequestHeader HttpHeaders head, @PathVariable(value = "id") UUID billid, @RequestParam("file") MultipartFile f) throws IOException, NoSuchAlgorithmException {
        stats.incrementCounter("endpoint.addFiles3.http.post");
        long startTime = System.currentTimeMillis();
        byte decoded[] = Base64.getDecoder().decode(head.getFirst("authorization").substring(6));
        String decodedstring = new String(decoded);
        String login[] = decodedstring.split(":");
        System.out.println(login[0]);
        System.out.println(login[1]);
        User u = userRepository.findByEmail(login[0]);
        if (u == null) {
            stats.recordExecutionTime("PostBillLatency", System.currentTimeMillis() - startTime);
            logger.warn("Email not exists in the data base",logger.getClass());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not in database!!!");
        }
        if (f == null) {
            stats.recordExecutionTime("PostBillLatency", System.currentTimeMillis() - startTime);
            logger.warn("Empty file",logger.getClass());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please attach a file");
        }

        if (BCrypt.checkpw(login[1], u.getPassword())) {
            Bill b = billRepository.findById(billid);
            if (b == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not FOUND!!!");
            if (!b.getOwnerId().equals(u.getId()))
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("UNAUTHORIZED!!!");
            com.cloudcomp.Pojo.File file = fileRepository.findByBillid(billid);
            if (file != null)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Files already exists.Delete it before you rewrite");
            if ( f.getOriginalFilename().endsWith(".pdf") || f.getOriginalFilename().endsWith(".png") || f.getOriginalFilename().endsWith(".jpg") || f.getOriginalFilename().endsWith(".jpeg")) {

                String filename = f.getOriginalFilename();
                String suffix = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
                String newFileName = System.currentTimeMillis() + "." + suffix;

                AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();
                try {
                    String hash="";

                    InputStream is = f.getInputStream();
                    s3.putObject(new PutObjectRequest(environment.getProperty("bucket.name"), newFileName, is, new ObjectMetadata()).withCannedAcl(CannedAccessControlList.Private));
                    String url = S3Util.productRetrieveFileFromS3("", newFileName, environment.getProperty("bucket.name"));
                    AmazonS3 s3client = AmazonS3ClientBuilder.standard().build();
                    for (S3ObjectSummary summary : S3Objects.inBucket(s3client, environment.getProperty("bucket.name"))) {
                        hash = summary.getETag();
                    }
                    File file1 = new File();
                    file1.setBillid(billid);
                    file1.setFile_name(f.getOriginalFilename());
                    file1.setDate(new Date().toString());
                    file1.setUrl(url);
                    file1.setMd5(hash);
                    file1.setMimeType(f.getContentType());

                    file1.setFilesize(f.getSize());
                    b.setAttachment(file1);
                    fileRepository.save(file1);
                    Gson gson = new Gson();
                    String json = gson.toJson(file1);
                    return ResponseEntity.status(HttpStatus.OK).body(json);
                } catch (AmazonServiceException e) {
                    System.err.println(e.getErrorMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getErrorMessage());
                }
            }
            else
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Wrong file format");
        }
        stats.recordExecutionTime("PostBillLatency", System.currentTimeMillis() - startTime);
        logger.warn("Email not exists in the data base",logger.getClass());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please use correct login details!!!");
    }

    @GetMapping(path = "v1/bill/{bid}/file/{fid}", produces = "application/json")
    public ResponseEntity<String> getFile(@RequestHeader HttpHeaders head, @PathVariable(value = "bid") UUID billid, @PathVariable(value = "fid") UUID fileid) {
        byte decoded[] = Base64.getDecoder().decode(head.getFirst("authorization").substring(6));
        String decodedstring = new String(decoded);
        String login[] = decodedstring.split(":");
        User u = userRepository.findByEmail(login[0]);
        if (u == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not in database!!!");
        }
        if (BCrypt.checkpw(login[1], u.getPassword())) {
            Bill b = billRepository.findById(billid);
            if (b == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not FOUND!!!");
            if (!b.getOwnerId().equals(u.getId()))
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("UNAUTHORIZED!!!");
            else {

                com.cloudcomp.Pojo.File fi = fileRepository.findById(fileid);
                if (fi == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("NO such file exists");
                }
                if (fi.getBillid().equals(billid)) {
                    Gson gson = new Gson();
                    String json = gson.toJson(fi);
                    return ResponseEntity.status(HttpStatus.OK).body(json);
                } else
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("This billid doesnt have any files");
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please use correct login details!!!");

    }

    @DeleteMapping(path = "v1/bill/{bid}/file/{fid}")
    public ResponseEntity<String> deleteFile(@RequestHeader HttpHeaders head, @PathVariable(value = "bid") UUID billid, @PathVariable(value = "fid") UUID fileid) throws IOException {
        byte decoded[] = Base64.getDecoder().decode(head.getFirst("authorization").substring(6));
        String decodedstring = new String(decoded);
        String login[] = decodedstring.split(":");
        User u = userRepository.findByEmail(login[0]);
        if (u == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not in database!!!");
        }
        if (BCrypt.checkpw(login[1], u.getPassword())) {
            Bill b = billRepository.findById(billid);
            if (b == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not FOUND!!!");
            if (!b.getOwnerId().equals(u.getId()))
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("UNAUTHORIZED!!!");
            else {
                com.cloudcomp.Pojo.File fi = fileRepository.findById(fileid);
                if (fi == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No such file exists");
                }
                if (fi.getBillid().equals(billid)) {
                    String[] val = fi.getUrl().split("/" + environment.getProperty("bucket.name"));
                    String[] key = val[1].split("/");
                    AmazonS3 s3client = AmazonS3ClientBuilder.standard().build();
                    String deleledFile = "";
                    for (S3ObjectSummary summary : S3Objects.inBucket(s3client, environment.getProperty("bucket.name"))) {
                        String imageName = summary.getKey();
                        if (imageName.equals(key[1])) {
                            deleledFile = imageName;
                            b.setAttachment(null);
                            fileRepository.delete(fi);

                            break;
                        }
                    }
                    if(!deleledFile.equals("")){
                        s3client.deleteObject(environment.getProperty("bucket.name"), deleledFile);
                    }
                    return ResponseEntity.status(HttpStatus.OK).body("File deleted successfully");
                }
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please use correct login details!!!");
    }

//    private static String getFileChecksum(MessageDigest digest, MultipartFile file) throws IOException
//    {
//        java.io.File convFile = new java.io.File(file.getOriginalFilename());
//        convFile.createNewFile();
//        try(InputStream is = file.getInputStream()) {
//            Files.copy(is, convFile.toPath());
//        }
//
//        FileInputStream fis = new FileInputStream(convFile);
//
//        byte[] byteArray = new byte[1024];
//        int bytesCount = 0;
//
//        while ((bytesCount = fis.read(byteArray)) != -1) {
//            digest.update(byteArray, 0, bytesCount);
//        };
//
//        fis.close();
//        byte[] bytes = digest.digest();
//
//        StringBuilder sb = new StringBuilder();
//        for(int i=0; i< bytes.length ;i++)
//        {
//            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
//        }
//        return sb.toString();
//    }
}