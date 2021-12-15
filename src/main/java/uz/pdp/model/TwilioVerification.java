package uz.pdp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TwilioVerification {
    private UUID id=UUID.randomUUID();
    private TgUser user;
    private String code;
    private boolean verified;

    public TwilioVerification(TgUser user) {
        this.user = user;
    }
}
