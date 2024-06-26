package backend.auth;

import backend.models.dtoRequest.AuthenticationRequest;
import backend.models.dtoResponse.AuthenticationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestBody AuthenticationRequest request) {
        try {
            AuthenticationResponse response = service.authenticate(request);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request){
        //return ResponseEntity.ok(service.register(request));
        try {
            service.register(request);
            return ResponseEntity.ok("Usuario registrado, confirme su mail para continuar.");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        }
    }

    @GetMapping("/confirm_register")
    public ResponseEntity<?> validateRegister(@RequestParam("token") String token){
        try {
            service.validateRegisterRequest(token);
            return ResponseEntity.status(HttpStatus.OK).body("Usuario dado de alta");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ex.getMessage());
        }
    }
}
