package me.dolia.metronome;

import java.awt.*;
import java.awt.event.ActionListener;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.*;

/**
 * Metronome App.
 *
 * @author Maksym Dolia
 */
public class MetronomeApp extends JFrame {

    private static final String START = "Start";

    private final transient Metronome metronome;

    // Swing variables
    private JSpinner spinnerBPM;
    private JSpinner spinnerBeat;
    private JButton startButton;
    private JRadioButton[] valueButtons;

    public MetronomeApp(Metronome metronome) {
        super("Metronome");
        this.metronome = metronome;
        initSwingComponents();
        initListeners();
    }

    private void initSwingComponents() {

        initMenu();

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // panel with spinners
        // add spinner for bpm control
        JPanel upperPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        upperPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        JLabel labelBPM = new JLabel("Tempo in BPM:", SwingConstants.RIGHT);
        upperPanel.add(labelBPM);

        SpinnerModel spinnerModelBPM = new SpinnerNumberModel(120, 0, 240, 1);
        spinnerBPM = new JSpinner(spinnerModelBPM);
        upperPanel.add(spinnerBPM);

        JLabel labelBeat = new JLabel("Beat:", SwingConstants.RIGHT);
        upperPanel.add(labelBeat);

        SpinnerModel spinnerModelBeat = new SpinnerNumberModel(metronome.getBeat(),
                0, 9, 1);
        spinnerBeat = new JSpinner(spinnerModelBeat);
        upperPanel.add(spinnerBeat);
        mainPanel.add(upperPanel, BorderLayout.NORTH);

        ButtonGroup valueGroup = new ButtonGroup();
        JPanel panelValue = new JPanel(new GridLayout(2, 4));
        panelValue.setBorder(BorderFactory.createTitledBorder("Value"));
        valueButtons = new JRadioButton[RhythmicPattern.values().length];

        for (int i = 0; i < RhythmicPattern.values().length; i++) {
            valueButtons[i] = new JRadioButton("", i == 0);
            Icon icon = new ImageIcon(MetronomeApp.class.getResource("/" + RhythmicPattern.values()[i].name().toLowerCase() + ".png"));
            JLabel label = new JLabel(icon);
            JPanel panel = new JPanel(new FlowLayout());
            panel.add(valueButtons[i]);
            panel.add(label);
            panelValue.add(panel);
            valueGroup.add(valueButtons[i]);
        }

        mainPanel.add(panelValue, BorderLayout.CENTER);

        // create button
        startButton = new JButton(START);
        mainPanel.add(startButton, BorderLayout.SOUTH);
        getContentPane().add(mainPanel);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null); // place in the middle of the screen
        pack();
    }

    private int getBPM() {
        return (int) spinnerBPM.getValue();
    }

    private void initListeners() {
        spinnerBPM.addChangeListener(e -> metronome.setBPM(getBPM()));

        spinnerBeat.addChangeListener(e -> {
            int beat = (int) spinnerBeat.getValue();
            try {
                metronome.setBeat(beat);
            } catch (InvalidMidiDataException e1) {
                showErrorMessageAndExit(e1);
            }
        });

        startButton.addActionListener(e -> {
            String command = e.getActionCommand();

            if (START.equals(command)) {
                startButton.setText("Stop");
                try {
                    metronome.play();
                } catch (InvalidMidiDataException e1) {
                    showErrorMessageAndExit(e1);
                }
            } else if ("Stop".equals(command)) {
                startButton.setText(START);
                metronome.stop();
            }

        });

        ActionListener valueButtonsListener = e -> {
            String value = e.getActionCommand();
            try {
                metronome.setPattern(RhythmicPattern.valueOf(value));
            } catch (InvalidMidiDataException e1) {
                showErrorMessageAndExit(e1);
            }
        };

        for (int i = 0; i < valueButtons.length; i++) {
            valueButtons[i].setActionCommand(RhythmicPattern.values()[i].name());
            valueButtons[i].addActionListener(valueButtonsListener);
        }
    }

    private void initMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu infoMenu = new JMenu("Info");
        final JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> {
            String text = "Hi!\nWelcome to the metronome app. \nI wrote this program for fun, and I hope you will enjoy it as well. \n\nCheers,\nMaks \n\nmaksym (dot) dolia (at) gmail (dot) com";
            JOptionPane.showMessageDialog(null, text, aboutItem.getText(),
                    JOptionPane.INFORMATION_MESSAGE);
        });
        infoMenu.add(aboutItem);
        menuBar.add(infoMenu);

        setJMenuBar(menuBar);
    }

    private void showErrorMessageAndExit(Exception e) {
        String errorMessage = "Error occured: " + e.getMessage()
                + "\nSystem will be closed.";
        JOptionPane.showMessageDialog(null, errorMessage, "Error",
                JOptionPane.ERROR_MESSAGE);
        System.exit(1); // exit with error
    }

    public static void main(String[] args) {

        try {
            final Metronome metronome = new Metronome();
            SwingUtilities.invokeLater(() -> {
                MetronomeApp demo = new MetronomeApp(metronome);
                demo.setVisible(true);
            });
        } catch (MidiUnavailableException | InvalidMidiDataException e) {
            System.err.println("Error occurred: " + e.getMessage()
                    + "\n The program will be closed.");
            System.exit(1);
        }
    }
}
