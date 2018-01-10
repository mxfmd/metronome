package me.dolia.metronome;

public interface Metronome {

  void play() throws MetronomeException;

  void stop() throws MetronomeException;

  void setPattern(RhythmicPattern pattern);

  int getBeat();

  void setBeat(int beat);

  void setBPM(int bpm);
}
