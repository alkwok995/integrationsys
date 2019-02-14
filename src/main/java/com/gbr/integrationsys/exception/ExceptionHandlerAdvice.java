package com.gbr.integrationsys.exception;

import com.gbr.integrationsys.util.PrivateMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
@ResponseBody
public class ExceptionHandlerAdvice {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandlerAdvice.class);

    @ExceptionHandler(Exception.class)
    public PrivateMsg handleException(Exception e) {
        String msg = "";
        if(e instanceof ImportException) {
            msg = ((ImportException) e).getShortMsg();
        } else {
            msg = e.getMessage();
        }
        PrivateMsg privateMsg = new PrivateMsg("customer", msg);
        privateMsg.setSuccess("success=no");

        LOGGER.info("导入失败");
        return privateMsg;
    }

}
