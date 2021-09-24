package com.cloudcomp.controller;

import com.cloudcomp.Pojo.User;
import com.cloudcomp.Repository.UserRepository;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.timgroup.statsd.StatsDClient;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class Home {
    @Autowired
    UserRepository userrepo;
    String regex = "^(.+)@(.+)$";

    @Autowired
    Environment env;

    @Autowired(required = false)
    private StatsDClient stats;


    @GetMapping(path = "/")
    public String home() {

        return "Hello world";

    }

    private final static Logger logger = LoggerFactory.getLogger(Home.class);
    @PostMapping(path = "/v1/user", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<String>  postuser(@RequestBody User u) {
        String passwordregex = "((?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%]).{8,})";
        if (env.getActiveProfiles().equals("aws"))
            stats.incrementCounter("endpoint.postuser.http.post");
        long startTime = System.currentTimeMillis();
        if (u.getFirst_name() != null && u.getLast_name() != null && u.getPassword() != null && u.getEmail() != null) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(u.getEmail());
            if (!matcher.matches()) {
                stats.recordExecutionTime("createUserLatency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Strong email req");
            }
            if (userrepo.findByEmail(u.getEmail()) != null) {
                stats.recordExecutionTime("createUserLatency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(" email in use");
            }
            u.setAccount_created(new Date().toString());
            u.setAccount_updated(new Date().toString());
            String password = u.getPassword();
            Pattern p = Pattern.compile(passwordregex);
            Matcher matcher1 = p.matcher(u.getPassword());
            if (!matcher1.matches()) {
                stats.recordExecutionTime("createUserLatency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Strong pass req");
            }
            String realPassword = u.getPassword();
            String encryptedPassword = BCrypt.hashpw(realPassword, BCrypt.gensalt(10));
            System.out.println(encryptedPassword);
            u.setPassword(encryptedPassword);
            boolean matched = BCrypt.checkpw(realPassword, encryptedPassword);
            userrepo.save(u);
            User userdb = userrepo.findByEmail(u.getEmail());
            User returnUser =new User();
            returnUser.setFirst_name(userdb.getFirst_name());
            returnUser.setLast_name(userdb.getLast_name());
            returnUser.setEmail(userdb.getEmail());
            returnUser.setAccount_created(userdb.getAccount_created());
            returnUser.setAccount_updated(userdb.getAccount_updated());
            returnUser.setId(u.getId());
            Gson gson=new Gson();
            String json=gson.toJson(returnUser);
            if (env.getActiveProfiles().equals("aws"))
                stats.recordExecutionTime("createUserLatency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.CREATED).body(json);
        } else {
            stats.recordExecutionTime("createUserLatency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("enter all ");
        }

    }

    @GetMapping(path = "/v1/user/self", produces = "application/json")

    public ResponseEntity<String> getResponse(@RequestHeader HttpHeaders head) {

        if(env.getActiveProfiles().equals("aws"))
        stats.incrementCounter("endpoint.getResponse.http.get");
        long startTime = System.currentTimeMillis();
        byte decoded[] = Base64.getDecoder().decode(head.getFirst("authorization").substring(6));
        String decodedstring = new String(decoded);

        String login[] = decodedstring.split(":");
        System.out.println(login[0]);
        System.out.println(login[1]);
        User u = userrepo.findByEmail(login[0]);
        if(u==null)
        {
            if(env.getActiveProfiles().equals("aws"))
            stats.recordExecutionTime("GetLatency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not in database!!!");
        }
        if (BCrypt.checkpw(login[1], u.getPassword())) {
            User user = new User();
            user.setId(u.getId());
            user.setAccount_updated(u.getAccount_updated());
            user.setAccount_created(u.getAccount_created());
            user.setLast_name(u.getLast_name());
            user.setFirst_name(u.getFirst_name());
            User userdb = userrepo.findByEmail(u.getEmail());
            User returnUser =new User();
            returnUser.setFirst_name(userdb.getFirst_name());
            returnUser.setLast_name(userdb.getLast_name());
            returnUser.setEmail(userdb.getEmail());
            returnUser.setAccount_created(userdb.getAccount_created());
            returnUser.setAccount_updated(userdb.getAccount_updated());
            returnUser.setId(userdb.getId());
            Gson gson=new Gson();
            String json=gson.toJson(returnUser);
            if(env.getActiveProfiles().equals("aws"))
            stats.recordExecutionTime("GetLatency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.OK).body(json);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please use correct login details!!!");
    }

    @PutMapping  (path = "/v1/user/self",consumes = "application/json")
    public ResponseEntity<String> putUser(@RequestHeader HttpHeaders head, @RequestBody User u)
    {
        stats.incrementCounter("endpoint.putUser.http.put");
        long startTime = System.currentTimeMillis();
        String passwordregex = "((?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%]).{8,})";
        byte decoded[] = Base64.getDecoder().decode(head.getFirst("authorization").substring(6));
        String decodedstring = new String(decoded);
        String login[] = decodedstring.split(":");
        User user = userrepo.findByEmail(login[0]);
        if(user==null)
        {
            stats.recordExecutionTime("Update Latency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not in database!!!");
        }
        boolean matched=BCrypt.checkpw(login[1], user.getPassword());
        if (matched) {
            stats.recordExecutionTime("Update Latency", System.currentTimeMillis() - startTime);
            if ( u.getAccount_created() != null || u.getAccount_updated() != null || u.getId()!=null) {
                stats.recordExecutionTime("Update Latency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You cannot edit those fields");
            }
            if(u.getFirst_name()==null || u.getLast_name()==null || u.getPassword()==null||u.getEmail()==null||!u.getEmail().equals(login[0])) {
                stats.recordExecutionTime("Update Latency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ALl fields req!!");
            }else
            {
                stats.recordExecutionTime("Update Latency", System.currentTimeMillis() - startTime);
                    user.setFirst_name(u.getFirst_name());
                    user.setLast_name(u.getLast_name());
                    Pattern p = Pattern.compile(passwordregex);
                    Matcher matcher1 = p.matcher(u.getPassword());
                    if (!matcher1.matches()) {
                        stats.recordExecutionTime("Update Latency", System.currentTimeMillis() - startTime);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("strong");
                    } else {
                        stats.recordExecutionTime("Update Latency", System.currentTimeMillis() - startTime);
                        String realPassword = u.getPassword();
                        String encryptedPassword = BCrypt.hashpw(realPassword, BCrypt.gensalt(10));
                        user.setPassword(encryptedPassword);
                    }

            }

                user.setAccount_updated(new Date().toString());
                userrepo.save(user);
            stats.recordExecutionTime("Update Latency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body("test");
            }
        stats.recordExecutionTime("Update Latency", System.currentTimeMillis() - startTime);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please use correct login details!!!");
        }

    }


