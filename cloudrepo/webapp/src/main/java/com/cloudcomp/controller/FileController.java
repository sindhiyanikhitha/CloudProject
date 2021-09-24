package com.cloudcomp.controller;

import com.cloudcomp.Pojo.Bill;
import com.cloudcomp.Pojo.User;
import com.cloudcomp.Repository.BillRepository;
import com.cloudcomp.Repository.FileRepository;
import com.cloudcomp.Repository.UserRepository;
import com.google.gson.Gson;
import com.timgroup.statsd.StatsDClient;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Profile("local")
@RestController
public class FileController {
    @Autowired
    UserRepository userRepository;
    @Autowired
    BillRepository billRepository;
    @Autowired
    FileRepository fileRepository;

    private static int counter = 0;


    @Autowired(required = false)
    private StatsDClient stats;

    private final static Logger logger = LoggerFactory.getLogger(FileController.class);

    @PostMapping(path = "v1/bill/{id}/file", produces = "application/json", consumes = "multipart/form-data")
    @ResponseBody
    public ResponseEntity<String> addFile(@RequestHeader HttpHeaders head, @PathVariable(value = "id") UUID billid, @RequestParam("file") MultipartFile f) throws IOException , NoSuchAlgorithmException {
        stats.incrementCounter("endpoint.addFile.http.post");
        long startTime = System.currentTimeMillis();
        byte decoded[] = Base64.getDecoder().decode(head.getFirst("authorization").substring(6));
        String decodedstring = new String(decoded);
        String login[] = decodedstring.split(":");
        System.out.println(login[0]);
        System.out.println(login[1]);
        User u = userRepository.findByEmail(login[0]);
        if (u == null) {
            stats.recordExecutionTime("PostFileLatency", System.currentTimeMillis() - startTime);
            logger.warn("Email not exists in the data base",logger.getClass());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not in database!!!");
        }
        if (f == null) {
            stats.recordExecutionTime("PostFileLatency", System.currentTimeMillis() - startTime);
            logger.warn("File not exists ",logger.getClass());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please attach a file");
        }

        if (BCrypt.checkpw(login[1], u.getPassword())) {
            Bill b = billRepository.findById(billid);
            if (b == null) {
                stats.recordExecutionTime("PostFileLatency", System.currentTimeMillis() - startTime);
                logger.warn("Email not exists in the data base",logger.getClass());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not FOUND!!!");
            }
            if (!b.getOwnerId().equals(u.getId())) {
                stats.recordExecutionTime("PostFileLatency", System.currentTimeMillis() - startTime);
                logger.warn("UNAUTHORIZED",logger.getClass());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("UNAUTHORIZED!!!");
            }
            com.cloudcomp.Pojo.File file=fileRepository.findByBillid(billid);
            if(file!=null)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Files already exists.Delete it before you rewrite");
            else {
                if (!f.isEmpty()) {
                    byte[] bytes = f.getBytes();


                    if ( f.getOriginalFilename().endsWith(".pdf") || f.getOriginalFilename().endsWith(".png") || f.getOriginalFilename().endsWith(".jpg") || f.getOriginalFilename().endsWith(".jpeg")) {
                        File dir = new File("tmpFiles");
                        String originalFileName = "";
                        if (!dir.exists())
                            dir.mkdirs();
                        Path path = Paths.get("tmpFiles/" + f.getOriginalFilename());
                        String s=Files.probeContentType(path);
                        System.out.println(s);
                        File serverFile = null;
                        if (!Files.exists(path)) {
                            serverFile = new File(dir.getAbsolutePath()
                                    + File.separator + f.getOriginalFilename());

                            BufferedOutputStream stream = new BufferedOutputStream(
                                    new FileOutputStream(serverFile));
                            stream.write(bytes);
                            stream.close();

                            MessageDigest md5Digest = MessageDigest.getInstance("MD5");


                            String checksum = getFileChecksum(md5Digest, serverFile);

                            System.out.println(checksum);

                            System.out.println("Server File Location="
                                    + serverFile.getAbsolutePath());
                            String mime=Files.probeContentType(path);
                            com.cloudcomp.Pojo.File fil = new com.cloudcomp.Pojo.File();
                            fil.setDate(new Date().toString());
                            fil.setBillid(billid);
                            fil.setUrl(serverFile.getAbsolutePath());
                            fil.setFile_name(f.getOriginalFilename());
                            fil.setMd5(checksum);
                            fil.setMimeType(mime);
                            fil.setFilesize(serverFile.length());
                            b.setAttachment(fil);
                            b.setUpdated_ts(new Date().toString());

                            fileRepository.save(fil);
                            billRepository.save(b);
                            Gson gson = new Gson();
                            String json = gson.toJson(fil);
                            return ResponseEntity.status(HttpStatus.OK).body(json);
                        } else {
                            int idxOfDot = f.getOriginalFilename().lastIndexOf('.');
                            String extension = f.getOriginalFilename().substring(idxOfDot + 1);
                            String name = f.getOriginalFilename().substring(0, idxOfDot);
                            //int counter = 0;
                            while (Files.exists(path)) {
                                counter++;
                                originalFileName = name + "(" + counter + ")." + extension;
                                path = Paths.get(originalFileName);
                            }
                            serverFile = new File(dir.getAbsolutePath() + File.separator + originalFileName);
                            BufferedOutputStream stream = new BufferedOutputStream(
                                    new FileOutputStream(serverFile));
                            stream.write(bytes);
                            stream.close();

                            MessageDigest md5Digest = MessageDigest.getInstance("MD5");

                            String checksum = getFileChecksum(md5Digest, serverFile);

                            String mime=Files.probeContentType(path);
                            System.out.println(checksum);
                            System.out.println("Server File Location="
                                    + serverFile.getAbsolutePath());
                            com.cloudcomp.Pojo.File fil = new com.cloudcomp.Pojo.File();
                            fil.setDate(new Date().toString());
                            fil.setBillid(b.getId());
                            fil.setMimeType(mime);
                            fil.setUrl(serverFile.getAbsolutePath());
                            fil.setFile_name(originalFileName);
                            fil.setMd5(checksum);
                            fil.setFilesize(serverFile.length());
                            b.setAttachment(fil);
                            b.setUpdated_ts(new Date().toString());

                            fileRepository.save(fil);
                            billRepository.save(b);
                            Gson gson = new Gson();
                            String json = gson.toJson(fil);

                            return ResponseEntity.status(HttpStatus.CREATED).body(json);
                        }
                    } else
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Wrong file format");

                } else
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please attach a file");
            }
            //return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please use correct login details!!!");
        }
        stats.recordExecutionTime("PostFileLatency", System.currentTimeMillis() - startTime);
        logger.warn("UNAUTHORIZED",logger.getClass());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please use correct login details!!!");
    }
    // return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please use correct login details!!!");

    @GetMapping(path = "v1/bill/{bid}/file/{fid}", produces = "application/json")
    public ResponseEntity<String> getFile(@RequestHeader HttpHeaders head, @PathVariable(value = "bid") UUID billid, @PathVariable(value = "fid") UUID fileid) {
        stats.incrementCounter("endpoint.getFile.http.get");
        long startTime = System.currentTimeMillis();
        byte decoded[] = Base64.getDecoder().decode(head.getFirst("authorization").substring(6));
        String decodedstring = new String(decoded);
        String login[] = decodedstring.split(":");
        User u = userRepository.findByEmail(login[0]);
        if (u == null) {
            stats.recordExecutionTime("getFileLatency", System.currentTimeMillis() - startTime);
            logger.warn("UNAUTHORIZED",logger.getClass());
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
        stats.recordExecutionTime("getFileLatency", System.currentTimeMillis() - startTime);
        logger.warn("UNAUTHORIZED",logger.getClass());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please use correct login details!!!");

    }

    @DeleteMapping(path = "v1/bill/{bid}/file/{fid}")
    public ResponseEntity<String> deleteFile(@RequestHeader HttpHeaders head, @PathVariable(value = "bid") UUID billid, @PathVariable(value = "fid") UUID fileid) throws IOException {
        stats.incrementCounter("endpoint.deleteFile.http.delete");
        long startTime = System.currentTimeMillis();
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
                    Path path=Paths.get("tmpFiles/"+ fi.getFile_name());
                    Files.delete(path);
                    b.setAttachment(null);
                    billRepository.save(b);
                    Long l=fileRepository.deleteById(fileid);
                    if(l==1) {
                        stats.recordExecutionTime("deleteFileLatency", System.currentTimeMillis() - startTime);
                        logger.info("deleted",logger.getClass());
                        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("done");
                    }
                    else
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("NOT FOUND ");
                }
            }
        }
        stats.recordExecutionTime("deleteFileLatency", System.currentTimeMillis() - startTime);
        logger.info("not in db",logger.getClass());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please use correct login details!!!");
    }

    private static String getFileChecksum(MessageDigest digest, File file) throws IOException
    {
        FileInputStream fis = new FileInputStream(file);

        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        };

        fis.close();
        byte[] bytes = digest.digest();

        StringBuilder sb = new StringBuilder();
        for(int i=0; i< bytes.length ;i++)
        {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }
}
