package com.cloudcomp.controller;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.cloudcomp.Pojo.Bill;
import com.cloudcomp.Pojo.BillSqs;
import com.cloudcomp.Pojo.File;
import com.cloudcomp.Pojo.User;
import com.cloudcomp.Repository.BillRepository;
import com.cloudcomp.Repository.FileRepository;
import com.cloudcomp.Repository.UserRepository;
import com.cloudcomp.metrics.ConfigSQS;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.timgroup.statsd.StatsDClient;

import org.apache.logging.log4j.util.Strings;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
public class BillController {
    @Autowired
    UserRepository userRepository;
    @Autowired
    BillRepository billRepository;

    @Autowired(required = false)
    FileRepository fileRepository;

    @Autowired(required = false)
    private StatsDClient stats;

    @Autowired(required = false)
    S3Controller s3Controller;

    @Autowired
    Environment environment;

    @Autowired(required = false)
    ConfigSQS sqs;

    private final static Logger logger = LoggerFactory.getLogger(BillController.class);

    @PostMapping(path = "/v1/bill", produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> addBill(@RequestHeader HttpHeaders head, @RequestBody Bill b) {
        stats.incrementCounter("endpoint.addBill.http.post");

        long startTime = System.currentTimeMillis();
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        byte decoded[] = Base64.getDecoder().decode(head.getFirst("authorization").substring(6));
        String decodedstring = new String(decoded);
        String login[] = decodedstring.split(":");
        System.out.println(login[0]);
        System.out.println(login[1]);
        User u = userRepository.findByEmail(login[0]);
        System.out.println(org.hibernate.Version.getVersionString());
        if (u == null) {
            stats.recordExecutionTime("PostBillLatency", System.currentTimeMillis() - startTime);
            logger.warn("Email not exists in the data base",logger.getClass());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not in database!!!");
        }

        if(Strings.isBlank(b.getVendor())||Strings.isBlank(b.getBill_date())||Strings.isBlank(b.getDue_date())||
                b.getAmount_due()<0.01||b.getCategories()==null||b.getCategories().size()<1||b.getPaymentStatus()==null){
            stats.recordExecutionTime("postBillLatency", System.currentTimeMillis() - startTime);
            logger.warn("Few fields missing",logger.getClass());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("enter all the fields");

        }

        try {
            formatter.parse(b.getBill_date());
            formatter.parse(b.getDue_date());
        } catch (ParseException e) {
            stats.recordExecutionTime("postBillLatency", System.currentTimeMillis() - startTime);
            logger.warn("wrong date format ",logger.getClass());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(" Wrong Date format ");
        }
        if (BCrypt.checkpw(login[1], u.getPassword())) {

            Bill bill = new Bill();
            bill.setOwnerId(u.getId());
            bill.setAmount_due(b.getAmount_due());
            bill.setBill_date(b.getBill_date());
            Set<String> set = new HashSet<>();
            for(String s : b.getCategories()){
                if(!set.add(s)){
                    stats.recordExecutionTime("PostBillLatency", System.currentTimeMillis() - startTime);
                    logger.warn("Add unique",logger.getClass());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please add unique categories");
                }
            }
            bill.setCategories(b.getCategories());
            bill.setCreated_ts(new Date().toString());
            bill.setDue_date(b.getDue_date());
            bill.setPaymentStatus(b.getPaymentStatus());
            bill.setUpdated_ts(new Date().toString());
            bill.setVendor(b.getVendor());
            billRepository.save(bill);
            stats.recordExecutionTime("PostBillLatency", System.currentTimeMillis() - startTime);
            JSONObject jsonObject = new JSONObject(bill);
            JSONObject Json = new JSONObject();
            //JsonArray jsonArray= new JsonArray();
            jsonObject.accumulate("attachement",Json );
           // bill.setAttachment (new File());
            stats.recordExecutionTime("PostBillLatency", System.currentTimeMillis() - startTime);
            logger.info("Bill added",logger.getClass());
            return ResponseEntity.status(HttpStatus.CREATED).body(jsonObject.toString());
        }
        stats.recordExecutionTime("PostBillLatency", System.currentTimeMillis() - startTime);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please use correct login details!!!");
    }

    @GetMapping(path = "/v1/bills", produces = "application/json")
    public ResponseEntity<String> getBills(@RequestHeader HttpHeaders head) {
        stats.incrementCounter("endpoint.getBills.http.get");
        long startTime = System.currentTimeMillis();
        byte decoded[] = Base64.getDecoder().decode(head.getFirst("authorization").substring(6));
        String decodedstring = new String(decoded);
        String login[] = decodedstring.split(":");
        System.out.println(login[0]);
        System.out.println(login[1]);
        JSONObject jsonObject=new JSONObject();
        User u = userRepository.findByEmail(login[0]);
        if (u == null) {
            stats.recordExecutionTime("getBillsLatency", System.currentTimeMillis() - startTime);
            logger.warn("Not in database",logger.getClass());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not in database!!!");
        }
        if (BCrypt.checkpw(login[1], u.getPassword())) {
            stats.recordExecutionTime("getBillsLatency", System.currentTimeMillis() - startTime);
            List<Bill> billList = billRepository.findByOwnerId(u.getId());
            JSONArray jsonArray = new JSONArray(billList);
            JSONArray jsonArray1 = new JSONArray();
            for(int i=0;i<billList.size();i++){
                JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                if(!jsonObject1.has("attachment")){
                    jsonObject1.accumulate("attachment",new JSONObject());
                }
                jsonArray1.put(jsonObject1);
            }

            Gson gson = new Gson();
            String json = gson.toJson(billList);
            stats.recordExecutionTime("getBillsLatency", System.currentTimeMillis() - startTime);
            logger.info("Retrieved",logger.getClass());
            return ResponseEntity.status(HttpStatus.OK).body(jsonArray1.toString());
            }
        stats.recordExecutionTime("getBillsLatency", System.currentTimeMillis() - startTime);
        return ResponseEntity.status(HttpStatus.OK).body("pass");
        }


    @GetMapping(path = "/v2/bills", produces = "application/json")
    public ResponseEntity<String> getBillsv2(@RequestHeader HttpHeaders head) {
        byte decoded[] = Base64.getDecoder().decode(head.getFirst("authorization").substring(6));
        String decodedstring = new String(decoded);
        String login[] = decodedstring.split(":");
        System.out.println(login[0]);
        System.out.println(login[1]);
        JSONObject jsonObject=new JSONObject();
        User u = userRepository.findByEmail(login[0]);
        if (u == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not in database!!!");
        }
        if (BCrypt.checkpw(login[1], u.getPassword())) {
            List<Bill> billList = billRepository.findByOwnerId(u.getId());
            JSONArray jsonArray = new JSONArray(billList);
            JSONArray jsonArray1 = new JSONArray();
            for(int i=0;i<billList.size();i++){
                JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                if(!jsonObject1.has("attachment")){
                    jsonObject1.accumulate("attachment",new JSONObject());
                }
                jsonArray1.put(jsonObject1);
            }

            Gson gson = new Gson();
            String json = gson.toJson(billList);
            return ResponseEntity.status(HttpStatus.OK).body(jsonArray1.toString());
        }
        return ResponseEntity.status(HttpStatus.OK).body("pass");
    }
        //return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please use correct login details!!!");


    @GetMapping(path = "/v1/bill/{id}", produces = "application/json")
    public ResponseEntity<String> getBill(@RequestHeader HttpHeaders head, @PathVariable(value = "id") UUID billid) {
        stats.incrementCounter("endpoint.getBill.http.get");
        long startTime = System.currentTimeMillis();
        byte decoded[] = Base64.getDecoder().decode(head.getFirst("authorization").substring(6));
        String decodedstring = new String(decoded);
        String login[] = decodedstring.split(":");
        User u = userRepository.findByEmail(login[0]);
        if (u == null) {
            stats.recordExecutionTime("getBillLatency", System.currentTimeMillis() - startTime);
            logger.warn("Not in database",logger.getClass());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not in database!!!");
        }
        if (BCrypt.checkpw(login[1], u.getPassword())) {
            stats.recordExecutionTime("getBillLatency", System.currentTimeMillis() - startTime);
            Bill b = billRepository.findById(billid);
            if (b == null) {
                stats.recordExecutionTime("getBillLatency", System.currentTimeMillis() - startTime);
                stats.recordExecutionTime("getBillsLatency", System.currentTimeMillis() - startTime);
                logger.warn("Not in database",logger.getClass());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not FOUND!!!");
            }
            if (!b.getOwnerId().equals(u.getId())) {
                stats.recordExecutionTime("getBillLatency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("UNAUTHORIZED!!!");
            }
//            File file=fileRepository.findByBillid(billid);
//            if(file!=null)
//                b.setAttachment(file);
            Gson gson = new Gson();
            String json = gson.toJson(b);
            stats.recordExecutionTime("getBillLatency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.OK).body(json);
        }
        stats.recordExecutionTime("getBillLatency", System.currentTimeMillis() - startTime);
        logger.warn("Not in database",logger.getClass());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please use correct login details!!!");

    }

    @PutMapping(path = "/v1/bill/{id}", consumes = "application/json")
    public ResponseEntity<String> putBill(@RequestHeader HttpHeaders head, @PathVariable(value = "id") UUID billid, @RequestBody Bill bill) {
        stats.incrementCounter("endpoint.putBill.http.put");
        long startTime = System.currentTimeMillis();
        byte decoded[] = Base64.getDecoder().decode(head.getFirst("authorization").substring(6));
        String decodedstring = new String(decoded);
        String login[] = decodedstring.split(":");
        Bill b = billRepository.findById(billid);
        System.out.println(b.toString());
        User u = userRepository.findByEmail(login[0]);
        if (u == null) {
            stats.recordExecutionTime("putBillLatency", System.currentTimeMillis() - startTime);
            logger.warn("Not in database",logger.getClass());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not in database!!!");
        }

        if ((BCrypt.checkpw(login[1], u.getPassword()))) {
            stats.recordExecutionTime("putBillLatency", System.currentTimeMillis() - startTime);
            if (bill.getCreated_ts() != null || bill.getUpdated_ts() != null || bill.getId() != null || bill.getCategories().size()<1) {
                stats.recordExecutionTime("putBillLatency", System.currentTimeMillis() - startTime);

                logger.warn("Not editable",logger.getClass());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You cannot edit those fields");
            }
            if ( bill.getBill_date() == null  || bill.getAmount_due() < 0.01 || bill.getDue_date() == null || bill.getPaymentStatus() ==null) {
                stats.recordExecutionTime("putBillsLatency", System.currentTimeMillis() - startTime);
                logger.warn("ALl fields required",logger.getClass());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ALl fields req!!");
            } else {
                if (b.getOwnerId().equals(u.getId())) {
                    stats.recordExecutionTime("putBillLatency", System.currentTimeMillis() - startTime);
                    System.out.println(b.toString());
                    b.setVendor(bill.getVendor());
                    b.setUpdated_ts(new Date().toString());
                    b.setPaymentStatus(bill.getPaymentStatus());
                    b.setDue_date(bill.getDue_date());
                    Set<String> set = new HashSet<>();
                    for(String s : b.getCategories()){
                        if(!set.add(s)){
                            stats.recordExecutionTime("putBillLatency", System.currentTimeMillis() - startTime);
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please add unique categories");
                        }
                    }
                    b.setCategories(bill.getCategories());
                    b.setBill_date(bill.getBill_date());
                    b.setAmount_due(bill.getAmount_due());
                    System.out.println(b.toString());
                    billRepository.save(b);
                    stats.recordExecutionTime("putBillLatency", System.currentTimeMillis() - startTime);
                    Gson gson = new Gson();
                    String json = gson.toJson(b);
                    stats.recordExecutionTime("putBillLatency", System.currentTimeMillis() - startTime);
                    return ResponseEntity.status(HttpStatus.OK).body(json);
                } else {

                    logger.warn("UNAUTHORIZED",logger.getClass());
                    stats.recordExecutionTime("putBillLatency", System.currentTimeMillis() - startTime);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("UNAUTHORIZED");
                }

            }
        }
        stats.recordExecutionTime("putBillLatency", System.currentTimeMillis() - startTime);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please use correct login details!!!");
    }

    @DeleteMapping(path = "/v1/bill/{id}")
    public ResponseEntity<String> deleteBill(@RequestHeader HttpHeaders head,@PathVariable(value = "id")UUID billid) throws IOException
    {
        stats.incrementCounter("endpoint.deleteBill.http.delete");
        long startTime = System.currentTimeMillis();
        byte decoded[] = Base64.getDecoder().decode(head.getFirst("authorization").substring(6));
        String decodedstring = new String(decoded);
        String login[] = decodedstring.split(":");
        Bill b = billRepository.findById(billid);
        User u = userRepository.findByEmail(login[0]);
        if (u == null) {
            stats.recordExecutionTime(" deleteLatency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not in database!!!");
        }
        if(b==null)
        {
            stats.recordExecutionTime("deleteLatency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("NOT FOUND ");
        }
        if (!b.getOwnerId().equals(u.getId())) {
            stats.recordExecutionTime("deleteLatency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("UNAUTHORIZED!!!");
        }

        if ((BCrypt.checkpw(login[1], u.getPassword()))) {
            stats.recordExecutionTime("deleteLatency", System.currentTimeMillis() - startTime);
            String env[] = environment.getActiveProfiles();
            List<String> result = Arrays.asList(env);
            if (result.contains("local") && fileRepository.findByBillid(billid) != null) {
                File file = fileRepository.findByBillid(billid);
                if (file != null) {
                    Path path = Paths.get("tmpFiles/" + file.getFile_name());
                    Files.delete(path);
                    Long file_delete = fileRepository.deleteByBillid(billid);
                    if (file_delete == 1)
                        System.out.println("file also deleted");
                }
                b.setAttachment(null);
                billRepository.save(b);
                stats.recordExecutionTime("deleteLatency", System.currentTimeMillis() - startTime);
                Long deleted = billRepository.deleteById(billid);
                if (deleted == 1)
                    return ResponseEntity.status(HttpStatus.NO_CONTENT).body("done");
                else
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("NOT FOUND ");
            }
            if(result.contains("aws") && fileRepository.findByBillid(billid)!=null)
            {
                s3Controller.deleteFile(head,billid,b.getAttachment().getId());
            }

            Long res=billRepository.deleteById(billid);
            if(res==1)
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body("done");
            else
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("NOT FOUND ");
            
        }
        stats.recordExecutionTime("deleteLatency", System.currentTimeMillis() - startTime);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please use correct login details!!!");
    }

    @GetMapping(path = "/v1/bills/due/{x}" ,produces = "application/json")
    public ResponseEntity<String> getDueBills(@RequestHeader HttpHeaders headers, @PathVariable(value="x") int x) {
        if (environment.getActiveProfiles().equals("aws"))
            stats.incrementCounter("endpoint.getDueBills.http.get");

        long startTime = System.currentTimeMillis();
        byte[] actualByte = Base64.getDecoder().decode(headers.getFirst("authorization").substring(6));
        String decodedToken = new String(actualByte);
        String[] credentials = decodedToken.split(":");
        User user = userRepository.findByEmail(credentials[0]);
        if(user==null) {
            logger.warn("User not found",logger.getClass());
            stats.recordExecutionTime("getDueBillsLatency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unauthorized user request failed");
        }
        logger.error(" Retrieving bills");


        if (BCrypt.checkpw(credentials[1], user.getPassword())) {
            List<Bill> bill = billRepository.findByOwnerId(user.getId());
            List<Bill> bills =new ArrayList<>();
            List<UUID> duebills = new ArrayList<>();



            for (int i = 0; i < bill.size(); i++) {
                String billDueDate= bill.get(i).getDue_date();
                Date todayDate = new Date();
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                String d1 =formatter.format(todayDate);
                DateTime date1 = new DateTime(d1);
                DateTime date2 = new DateTime(billDueDate);
                Days d =  Days.daysBetween(date1,date2);
                int diffDays =d.getDays();
                System.out.println(diffDays);
                if(diffDays>=0) {
                    if (diffDays <= x) {
                        bills.add(bill.get(i));
                    }
                }
                else
                    continue;
            }
            for (int i = 0; i < bills.size(); i++) {
                System.out.println(bills.get(i).getId());
                duebills.add(bills.get(i).getId());
            }

            BillSqs billSqs = new BillSqs();
            billSqs.setEmail(user.getEmail());
            billSqs.setBillList(duebills);

            queueEmail(billSqs);
            logger.info(" bills due ",logger.getClass());
            stats.recordExecutionTime("getDueBills", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.OK).body("please check your email for the due bills");
        }else{
            logger.warn("unauthorized user access",logger.getClass());
            stats.recordExecutionTime("getDueBills", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("wrong credentials ");
        }

    }

    public  ResponseEntity<String> queueEmail(BillSqs billSQSPojo){
        logger.warn("Email queue");
        logger.warn("Email queue sqs add ");
        logger.warn(sqs+"sqs object");
        String queueUrl = sqs.sqsClient().getQueueUrl("BillQueue").getQueueUrl();
        logger.warn("Queue URL"+queueUrl);
        SendMessageRequest send_msg_request = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(new Gson().toJson(billSQSPojo))
                .withDelaySeconds(0);
        sqs.sqsClient().sendMessage(send_msg_request);
        logger.warn("message sent");


        List<Message> messages = sqs.sqsClient().receiveMessage(queueUrl).getMessages();

        
        for (Message m : messages) {

            sendEmailBills(m.getBody());
            sqs.sqsClient().deleteMessage(queueUrl, m.getReceiptHandle());
        }

        return null;
    }

    public  ResponseEntity<String> sendEmailBills(String duebillPayload) {

        AmazonSNS amazonSNS = AmazonSNSClientBuilder.standard().withCredentials(new DefaultAWSCredentialsProviderChain()).build();
        amazonSNS.publish(new PublishRequest(amazonSNS.createTopic("bill_due_topic").getTopicArn(), duebillPayload ));
        logger.warn("message sent to SNS");
        logger.error("SUccessfully sent");
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}