
package com.cho.polio.presentation.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestSendReset {
    private String email;
    private String linkUrl;
}
