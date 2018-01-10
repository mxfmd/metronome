package me.dolia.metronome;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * Model for the metronome app..
 *
 * @author Maksym Dolia
 */
public class MIDIMetronome implements Metronome {

     /** Resolution set to 12, because this number can be divided by
      * 1 - quarter notes;
      * 2 - eight notes;
      * 3 - sixteenth notes
      * 4 - eight notes in triples
      * If you change this pattern you must change the track list arrays into
      * createAndSetTrack method! */
    private static final int RESOLUTION = 12;
    private static final int LOUD_NOTE = 44;
    private static final int QUIET_NOTE = 36;

    // main parameters
    private int bpm = 120;
    private int beat = 4;
    private RhythmicPattern pattern = RhythmicPattern.QUARTERS;

    // if main parameters have changed, then this assign true
    private boolean changed;

    // midi parameters
    private Sequencer sequencer;
    private Sequence sequence;
    private Track track;

    public MIDIMetronome(Sequencer sequencer) {
        try {
            this.sequencer = sequencer;
            this.sequencer.open();
            this.sequencer.setTempoInBPM(bpm);
            sequence = new Sequence(Sequence.PPQ, RESOLUTION);
            track = sequence.createTrack();
            this.sequencer.setSequence(sequence);
            this.sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            createAndSetTrack(beat, pattern);
        } catch (MidiUnavailableException | InvalidMidiDataException e) {
            throw new MetronomeException("Error occurred while trying to play", e);
        }
    }

    @Override
    public void setBPM(int bpm) {
        this.bpm = bpm;

        // Due to the bug in java machine (see http://www.jsresources.org/faq_midi.html#tempo_looping),
        // the sequencer reset the tempo, which had been set in BPM, after each end of loop.
        // So this is a little bit tricky way to set tempo to avoid problem.
        float current = sequencer.getTempoInBPM();
        float factor = bpm / current;
        sequencer.setTempoFactor(factor);
    }

    @Override
    public int getBeat() {
        return beat;
    }

    @Override
    public void setBeat(int beat) {
        this.beat = beat;
        changed = true;
        if (sequencer.isRunning()) {
            play();
        }
    }

    @Override
    public void setPattern(RhythmicPattern pattern) {
        this.pattern = pattern;
        changed = true;
        if (sequencer.isRunning()) {
            play();
        }
    }

    @Override
    public void play() {
        try {
            if (changed) {
                sequence.deleteTrack(track);        // delete previous track
                track = sequence.createTrack();
                createAndSetTrack(beat, pattern);
                sequencer.setSequence(sequence);
            }
            setBPM(bpm);
            sequencer.start();
        } catch (InvalidMidiDataException e) {
            throw new MetronomeException("Error occurred while trying to play", e);
        }
    }

    @Override
    public void stop() {
        sequencer.stop();
        sequencer.setMicrosecondPosition(0); // reset sequencer
    }

    private void createAndSetTrack(int beat, RhythmicPattern pattern)
            throws InvalidMidiDataException {

        int[] template = pattern.getTemplate();    // sort of "schema" for building sequence

        // creates the whole sample, which represents notes for "pattern", played "beat" times
        int sampleLengthInTicks = beat > 0 ? template.length * beat : RESOLUTION;
        int[] sample = new int[sampleLengthInTicks];
        for (int i = 0; i < sample.length; i++) {
            sample[i] = template[i % template.length];
        }

        // now create and add the necessary midi events to track
        for (int j = 0; j < sample.length; j++) {
            int note = j == 0 && beat > 0 ? LOUD_NOTE : QUIET_NOTE;    //if the note is first in sequence, then it is loud note.
            int velocity = j % RESOLUTION == 0 ? 127 : 90;        //if the note is the first in sequence, then velocity is max

            if (sample[j] != 0) {
                track.add(makeEvent(ShortMessage.NOTE_ON, 9, note, velocity, j));
                track.add(makeEvent(ShortMessage.NOTE_OFF, 9, note, velocity, j + 1));
            }
        }

        int lastTick = beat > 0 ? RESOLUTION * beat : RESOLUTION;
        track.add(makeEvent(ShortMessage.PROGRAM_CHANGE, 9, 1, 0, lastTick));
    }

    private MidiEvent makeEvent(int command, int channel, int note,
                                int velocity, int tick) throws InvalidMidiDataException {
        ShortMessage message = new ShortMessage(command, channel, note, velocity);
        return new MidiEvent(message, tick);
    }
}
