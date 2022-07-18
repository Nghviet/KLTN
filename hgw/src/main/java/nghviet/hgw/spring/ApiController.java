package nghviet.hgw.spring;

import org.springframework.web.bind.annotation.*;
import nghviet.hgw.utility.JWT;
import nghviet.hgw.spring.form.*;

import nghviet.hgw.threading.LoginSignal;

@RestController
@RequestMapping("/api")
public class ApiController {
    @GetMapping
    @RequestMapping(value = "/available")
    public String isAvailable() throws Exception {
        if(JWT.isAvailable()) return "True";
        throw new IllegalAccessException("Invalid user");
    }

    @PostMapping
    @RequestMapping(value = "/login")
    public String login(@RequestBody LoginForm loginForm) throws Exception {
        System.out.println(loginForm.username + ' ' + loginForm.password);
        int result = JWT.register(loginForm.username, loginForm.password, !JWT.isAvailable());
        if(result == 0) {
            LoginSignal.getInstance().doNotify();
            return "Login complete";
        }
        throw new IllegalAccessException("Invalid user");
    }
}
