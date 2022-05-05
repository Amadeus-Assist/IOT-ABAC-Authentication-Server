package com.columbia.iotabacserver.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.io.IOException;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UnexpectedHttpException extends IOException {
    private static final long serialVersionUID = -488634227197337154L;

    private HttpStatus status;
}
