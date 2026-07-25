package com.bemo.license.application;
import lombok.Getter;
@Getter public class LicenseException extends RuntimeException { private final String code; public LicenseException(String code,String message){super(message);this.code=code;} }
