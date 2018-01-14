package me.dolia.metronome.services;

public class MetronomeException extends RuntimeException {

  public MetronomeException() {
  }

  public MetronomeException(String message) {
    super(message);
  }

  public MetronomeException(String message, Throwable cause) {
    super(message, cause);
  }
}
