package me.dolia.metronome;

import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequencer;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Metronome App.
 *
 * @author Maksym Dolia
 */
public class MetronomeApp extends JFrame {

  private ResourceBundle messages = ResourceBundle.getBundle("messages");

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

    JPanel mainPanel = createMainPanel();

    // panel with spinners
    // add spinner for bpm control
    JPanel upperPanel = new JPanel(new GridLayout(1, 4, 5, 5));
    upperPanel.setBorder(createEmptyBorder(0, 0, 5, 0));
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
      Icon icon = new ImageIcon(MetronomeApp.class
          .getResource("/" + RhythmicPattern.values()[i].name().toLowerCase() + ".png"));
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

  private JPanel createMainPanel() {
    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBorder(createEmptyBorder(10, 10, 10, 10));
    return mainPanel;
  }

  private int getBPM() {
    return (int) spinnerBPM.getValue();
  }

  private static void showErrorMessage(Exception e) {
    String errorMessage = "Error occurred: " + e.getMessage();
    showMessageDialog(null, errorMessage, "Error", ERROR_MESSAGE);
  }

  public static void main(String[] args) {
    try {
      Sequencer sequencer = MidiSystem.getSequencer();
      final Metronome metronome = new MIDIMetronome(sequencer);
      SwingUtilities.invokeLater(() -> {
        MetronomeApp demo = new MetronomeApp(metronome);
        demo.setVisible(true);
      });
    } catch (Exception e) {
      showErrorMessage(e);
      System.exit(1);
    }
  }

  private void initListeners() {
    spinnerBPM.addChangeListener(e -> metronome.setBPM(getBPM()));

    spinnerBeat.addChangeListener(e -> {
      int beat = (int) spinnerBeat.getValue();
      metronome.setBeat(beat);
    });

    startButton.addActionListener(e -> {
      String command = e.getActionCommand();

      if (START.equals(command)) {
        startButton.setText("Stop");
        metronome.play();
      } else if ("Stop".equals(command)) {
        startButton.setText(START);
        metronome.stop();
      }

    });

    ActionListener valueButtonsListener = e -> {
      String value = e.getActionCommand();
      metronome.setPattern(RhythmicPattern.valueOf(value));
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
      String text = messages.getString("menu.about.info");
      showMessageDialog(null, text, aboutItem.getText(), INFORMATION_MESSAGE);
    });
    infoMenu.add(aboutItem);
    menuBar.add(infoMenu);

    setJMenuBar(menuBar);
  }
}
