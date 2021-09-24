package com.cloudcomp;

import com.cloudcomp.Pojo.User;
import com.cloudcomp.Repository.UserRepository;
import com.cloudcomp.controller.Home;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.regex.Matcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
class Csye6225ccApplicationTests {

    @InjectMocks
    Home userController;

    @Mock
    UserRepository userRepositry;
    @Mock
    Environment environment;


    @Test
    public void testAddUse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpHeaders head = new HttpHeaders();
        head.setBasicAuth("test123@hotmail.com","Test@cloud12");
        User user = new User();
        String[]profiles = {"aws"};
        when(userRepositry.findByEmail(any())).thenReturn(user);
        user.setFirst_name("first_name");
        user.setLast_name("last_name");
        user.setEmail("test123@hotmail.com");
        when(environment.getActiveProfiles()).thenReturn(profiles);
        String hashPassword = BCrypt.hashpw("Test@cloud12", BCrypt.gensalt(12));
        user.setPassword(hashPassword);
        ResponseEntity<String> responseEntity = userController.getResponse(head);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(200);


    }
}