package com.bootcamp19.helloworld.exception;

public class InvalidJumlahException extends RuntimeException {
    public InvalidJumlahException() {
        super("Jumlah quote harus antara 1 sampai 10");
    }
}
