package me.dolia.metronome;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * Model for the metronome app..
 * 
 * @author Maksym Dolia
 * 
 *
 */
public class MetronomeModel {

	// Resolution set to 12, because this number can be divided by
	// 1 - quater notes;
	// 2 - eight notes;
	// 3 - sixteenth notes
	// 4 - eight notes in tripples
	// If you change this value you must change the track list arrays into
	// createAndSetTrack method!
	public static final int RESOLUTION = 12;

	// main parameters
	private int bpm = 120;
	private int beat = 4;
	private String value = "quarters";

	// if main parameters have changed, then this assign true
	private boolean changed;

	// midi parameters
	private Sequencer sequencer;
	private Sequence sequence;
	private Track track;

	public MetronomeModel() throws MidiUnavailableException,
			InvalidMidiDataException {
		initMidi();
	}

	public int getBPM() {
		return bpm;
	}

	public void setBPM(int bpm) {
		this.bpm = bpm;
		
		// Due to the bug in java machine (see http://www.jsresources.org/faq_midi.html#tempo_looping),
		// the sequencer reset the tempo, which had been set in BPM, after each end of loop.
		// So this is a little bit tricky way to set tempo to avoid problem.
		float fCurrent = sequencer.getTempoInBPM();
		float fFactor = bpm / fCurrent;
		sequencer.setTempoFactor(fFactor);
	}

	public int getBeat() {
		return beat;
	}

	public void setBeat(int beat) throws InvalidMidiDataException {
		this.beat = beat;
		changed = true;
		if (sequencer.isRunning()) {
			startBeat();
		}
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) throws InvalidMidiDataException {
		this.value = value;
		changed = true;
		if (sequencer.isRunning()) {
			startBeat();
		}
	}

	private void initMidi() throws MidiUnavailableException,
			InvalidMidiDataException {
		sequencer = MidiSystem.getSequencer();
		sequencer.open();
		sequencer.setTempoInBPM(getBPM());
		sequence = new Sequence(Sequence.PPQ, RESOLUTION);
		track = sequence.createTrack();
		sequencer.setSequence(sequence);
		sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
		createAndSetTrack(beat, value);
	}

	public void startBeat() throws InvalidMidiDataException {
		if (changed) {
			sequence.deleteTrack(track);		// delete previous track
			track = sequence.createTrack();
			createAndSetTrack(beat, value);
			sequencer.setSequence(sequence);
		}
		setBPM(getBPM());
		sequencer.start();
	}

	public void stopBeat() {
		sequencer.stop();
		sequencer.setMicrosecondPosition(0); // reset sequencer
	}

	private void createAndSetTrack(int beat, String value)
			throws InvalidMidiDataException {

		int typNote = 36;	// "typical" note
		int loudNote = 44;	// first note of sequence

		int[] QUARTERS_TRACK_LIST = { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		int[] EIGHTS_TRACK_LIST = { 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0 };
		int[] TRIPPLE_EIGHTS_TRACK_LIST = { 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0 };
		int[] SIXTEENTHS_TRACK_LIST = { 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0 };
		int[] EIGHT_DOT_SIXTEENTH_TRACK_LIST = { 1, 0, 0, 0, 0, 0, 0, 0, 0, 1,
				0, 0 };
		int[] EIGHT_TWO_SIXTEENTHS_TRACK_LIST = { 1, 0, 0, 0, 0, 0, 1, 0, 0, 1,
				0, 0 };
		int[] TWO_SIXTEENTHS_EIGHT_TRACK_LIST = { 1, 0, 0, 1, 0, 0, 1, 0, 0, 0,
				0, 0 };

		int[] template = null;	// sort of "schema" for building sequence

		switch (value) {
		case "quarters":
			template = QUARTERS_TRACK_LIST;
			break;
		case "eighths":
			template = EIGHTS_TRACK_LIST;
			break;
		case "tripple-eighths":
			template = TRIPPLE_EIGHTS_TRACK_LIST;
			break;
		case "sixteenths":
			template = SIXTEENTHS_TRACK_LIST;
			break;
		case "eighth-dot-sixteenth":
			template = EIGHT_DOT_SIXTEENTH_TRACK_LIST;
			break;
		case "eighth-two-sixteenths":
			template = EIGHT_TWO_SIXTEENTHS_TRACK_LIST;
			break;
		case "two-sixteenths-eight":
			template = TWO_SIXTEENTHS_EIGHT_TRACK_LIST;
			break;
		default:
			return;
		}

		// creates the whole sample, which represents notes for "value", played "beat" times
		int sampleLengthInTicks = beat > 0 ? template.length * beat
				: RESOLUTION;
		int[] sample = new int[sampleLengthInTicks];
		for (int i = 0; i < sample.length; i++) {
			sample[i] = template[i % template.length];
		}

		// now create and add the necessary midi events to track
		for (int j = 0; j < sample.length; j++) {
			int note = j == 0 && beat > 0 ? loudNote : typNote;	//if the note is first in sequence, then it is loud note.
			int velocity = j % RESOLUTION == 0 ? 127 : 90;		//if the note is the first in sequence, then velocity is max

			if (sample[j] != 0) {
				track.add(makeEvent(ShortMessage.NOTE_ON, 9, note, velocity, j));
				track.add(makeEvent(ShortMessage.NOTE_OFF, 9, note, velocity,
						j + 1));
			}
		}

		int lastTick = beat > 0 ? RESOLUTION * beat : RESOLUTION;
		track.add(makeEvent(ShortMessage.PROGRAM_CHANGE, 9, 1, 0, lastTick));
	}

	private MidiEvent makeEvent(int command, int channel, int note,
			int velocity, int tick) throws InvalidMidiDataException {

		MidiEvent event = null;
		ShortMessage message = new ShortMessage(command, channel, note,
				velocity);
		event = new MidiEvent(message, tick);
		return event;

	}

}