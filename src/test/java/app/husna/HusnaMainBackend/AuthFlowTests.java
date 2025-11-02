package app.husna.HusnaMainBackend;

import app.husna.HusnaMainBackend.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserService users;

    @BeforeEach
    void setUp() {
        // ensure a user exists for login
        try {
            users.signUp("demo@example.com", null, "demo-user");
        } catch (IllegalStateException ignore) {
            // already created
        }
    }

    @Test
    void loginPageLoads() throws Exception {
        String html = mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Log in");
    }

    @Test
    void requestOtpForExistingUserShowsVerifyFragment() throws Exception {
        String fragment = mvc.perform(post("/auth/otp/request")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("contact=demo@example.com"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(fragment).contains("Enter code");
    }

    @Test
    void signupCreatesAccount() throws Exception {
        String fragment = mvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("email=newuser@example.com&username=newbie"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(fragment).contains("Account created");
    }
}
