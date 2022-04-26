package simulator.launcher;

import org.apache.commons.cli.*;
import simulator.control.Controller;
import simulator.factories.*;
import simulator.model.DequeuingStrategy;
import simulator.model.Event;
import simulator.model.LightSwitchingStrategy;
import simulator.model.TrafficSimulator;
import simulator.view.MainWindow;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Main {

	enum ExeMode { GUI, BATCH };
	private static ExeMode _mode = ExeMode.GUI;

	private final static Integer _timeLimitDefaultValue = 10;
	private static String _inFile = null;
	private static String _outFile = null;
	private static Factory<Event> _eventsFactory = null;
	private static int _timeLimit;

	private static void parseArgs(String[] args) {

		// define the valid command line options
		//
		Options cmdLineOptions = buildOptions();

		// parse the command line as provided in args
		//
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine line = parser.parse(cmdLineOptions, args);
			parseHelpOption(line, cmdLineOptions);
			parseInFileOption(line);
			parseOutFileOption(line);
			parseTimeOption(line);

			// if there are some remaining arguments, then something wrong is
			// provided in the command line!
			//
			String[] remaining = line.getArgs();
			if (remaining.length > 0) {
				String error = "Illegal arguments:";
				for (String o : remaining)
					error += (" " + o);
				throw new ParseException(error);
			}

		} catch (ParseException e) {
			System.err.println(e.getLocalizedMessage());
			System.exit(1);
		}

	}

	private static Options buildOptions() {
		Options cmdLineOptions = new Options();

		cmdLineOptions.addOption(Option.builder("i").longOpt("input").hasArg().desc("Events input file").build());
		cmdLineOptions.addOption(Option.builder("o").longOpt("output").hasArg().desc("Output file, where reports are written.").build());
		cmdLineOptions.addOption(Option.builder("h").longOpt("help").desc("Help").build());
		cmdLineOptions.addOption(Option.builder("t").longOpt("ticks").hasArg().desc("Ticks").build());
		//cmdLineOptions.addOption(Option.builder("m").longOpt("mode").desc("").build());

		return cmdLineOptions;
	}

	private static void parseHelpOption(CommandLine line, Options cmdLineOptions) {
		if (line.hasOption("h")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(Main.class.getCanonicalName(), cmdLineOptions, true);
			System.exit(0);
		}
	}
	
	private static void parseTimeOption (CommandLine line) throws ParseException {
		
		if (!line.hasOption("t"))
			_timeLimit  = _timeLimitDefaultValue;
		else
			_timeLimit = Integer.parseInt(line.getOptionValue("t"));
	}

	private static void parseInFileOption(CommandLine line) throws ParseException {
		_inFile = line.getOptionValue("i");
		if (_inFile == null && _mode == ExeMode.BATCH) {
			throw new ParseException("An events file is missing");
		}
	}

	private static void parseOutFileOption(CommandLine line) throws ParseException {
		if(_mode != ExeMode.GUI)
		_outFile = line.getOptionValue("o");
	}

	private static void initFactories() {
		
		List<Builder<LightSwitchingStrategy>> lsbs = new ArrayList<>();
		lsbs.add( new RoundRobinStrategyBuilder() );
		lsbs.add( new MostCrowdedStrategyBuilder() );
		Factory<LightSwitchingStrategy> lssFactory = new BuilderBasedFactory<>(lsbs);
		
		List<Builder<DequeuingStrategy>> dqbs = new ArrayList<>();
		dqbs.add( new MoveFirstStrategyBuilder() );
		dqbs.add( new MoveAllStrategyBuilder() );
		Factory<DequeuingStrategy> dqsFactory = new BuilderBasedFactory<>(dqbs);
		
		List<Builder<Event>> ebs = new ArrayList<>();
		ebs.add( new NewJunctionEventBuilder(lssFactory,dqsFactory) );
		ebs.add( new NewCityRoadEventBuilder() );
		ebs.add( new NewInterCityRoadEventBuilder() );
		ebs.add(new NewVehicleEventBuilder());
		ebs.add (new SetWeatherEventBuilder());
		ebs.add(new SetContClassEventBuilder());
		
		_eventsFactory = new BuilderBasedFactory<>(ebs);
		
	}

	private static void startBatchMode() throws IOException {
		InputStream in = new FileInputStream (new File (_inFile));
		OutputStream out = null; 
		TrafficSimulator ts = new TrafficSimulator();
		Controller control = new Controller(ts, _eventsFactory);
		
		if (_outFile == null)
			out = System.out;
		else
			out = new FileOutputStream(new File (_outFile));
		
		control.loadEvents(in);
		in.close();
		control.run(_timeLimit, out);
	}

	private static void startGUIMode() throws IOException {
		

		TrafficSimulator ts = new TrafficSimulator();
		Controller control = new Controller(ts, _eventsFactory);
		
		if (_inFile != null) {
			
			InputStream in = new FileInputStream (new File(_inFile));
			control.loadEvents(in);
			in.close();
		}
		
		//OutputStream out = _outFile == null ? System.out : new FileOutputStream(_outFile);
		//control.run(_timeLimit, out);
		

		SwingUtilities.invokeLater( () -> new MainWindow(control));

		System.out.println("Done!");
	}

	private static void start(String[] args) throws IOException {
		initFactories();
		parseArgs(args);

		switch(_mode){
			case GUI :
				startGUIMode();
				break;
			case BATCH:
				startBatchMode();
				break;
			default :
				break;
		}
		//startBatchMode();
	}

	// example command lines:
	//
	// -i resources/examples/ex1.json
	// -i resources/examples/ex1.json -t 300
	// -i resources/examples/ex1.json -o resources/tmp/ex1.out.json
	// --help

	public static void main(String[] args) {
		try {
			start(args);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
