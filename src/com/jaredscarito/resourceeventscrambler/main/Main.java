package com.jaredscarito.resourceeventscrambler.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final public class Main {
	
	private static String[] systemResources;
	private static String[] fivemEvents;
	private static String[] blacklistedFiles;
	private static Main instance; // Used to call non-static events

	// Private constructor to prevent instantiation more than once.

	private Main() {
		instance = this;
	}

	private static boolean isFivemEvent(String eventName) {
		for (String fivemEvent : fivemEvents) {
			if (eventName.equals(fivemEvent)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isSystemResource(String fileName) {
		for (String systemResource : systemResources) {
			if (fileName.equals(systemResource)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isBlacklistedFile(File f) {
		for (String filePath : blacklistedFiles)
			if (filePath.equals(f.getAbsolutePath()))
				return true;
		return false;
	}

	private static void getConfigValues() {
		ArrayList<String> systemResources = new ArrayList<>();
		ArrayList<String> fivemEvents = new ArrayList<>();
		ArrayList<String> blacklistedFiles = new ArrayList<>();
		File f = new File("config");
		if (!f.exists()) {
			System.out.println("No Config Detected!  Creating Config.");
			f = createNewFile();
			
		}
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(f.getAbsolutePath()));
			// Keeps track of which arrayList to add to
			byte arr = -1;
			for (String s = reader.readLine(); s != null; s = reader.readLine()) {
				// Ignore my Homemade Comment Lines
				if (s.startsWith("#"))
					continue;
				if (s.equals("Blacklisted Resources:")) {
					arr = 0;
					continue;
				}
				if (s.equals("Blacklisted Events:")) {
					arr = 1;
					continue;
				}
				if (s.equals("Blacklisted Files:")) {
					arr = 2;
					continue;
				}
				switch (arr) {
				case 0:
					systemResources.add(s);
					break;
				case 1:
					fivemEvents.add(s);
					break;
				case 2:
					blacklistedFiles.add(new File("resources/" + s).getAbsolutePath());
				}
			}
			Main.systemResources = systemResources.toArray(new String[systemResources.size()]);
			Main.fivemEvents = fivemEvents.toArray(new String[fivemEvents.size()]);
			Main.blacklistedFiles = blacklistedFiles.toArray(new String[blacklistedFiles.size()]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static File createNewFile() {
		FileOutputStream fooStream = null;
		try {
			f.createNewFile();
			fooStream = new FileOutputStream(f, false);
			InputStream inStream = getInternalConfigStream();
			for(int data = inStream.read(); data != -1; data = inStream.read()) {
				fooStream.write(data);
			}
			inStream.close();
			fooStream.close();
			return f;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static InputStream getInternalConfigStream() throws IOException {
		return instance.getClass().getClassLoader()
				.getResourceAsStream("com/jaredscarito/resourceeventscrambler/config");
	}

	private static HashMap<String, String> keyVals = new HashMap<>();

	public static void main(String[] args) {
		// Main thread
		System.out.println("[ResourceEventScrambler] Running Scrambler");
		new Main();
		getConfigValues();
		List<File> resources = listf("resources");
		File resourceDir = new File("resources");
		copyFolder(resourceDir, new File("resources [UNSCRAMBLED]"));
		System.out.println("[ResourceEventScrambler] Created folder: resources [UNSCRAMBLED]");
		for (File f : resources) {
			if (f.getName().toLowerCase().contains(".lua")) {
				// It's a Lua file
				if (!f.getName().contains("__resource") && !f.getName().contains("fxmanifest")) {
					if (isBlacklistedFile(f)) {
						System.out.println("[ResourceEventScrambler] Skipped Blacklisted File: " + f.getAbsolutePath());
						continue;
					}
					// Not the resource lua, we can scramble the events of this file
					// Searches:
					/**
					 * this.addEventHandlerRe =
					 * /AddEventHandler\((\n["'](.*?)["']|\n\s+["'](.*?)["']|.+["'](.*?)["']|["'](.*?)["'])/g;
					 * this.triggerEventRe =
					 * /TriggerEvent\((\n["'](.*?)["']|\n\s+["'](.*?)["']|.+["'](.*?)["']|["'](.*?)["'])/g;
					 * this.registerServerEventRe =
					 * /RegisterServerEvent\((\n["'](.*?)["']|\n\s+["'](.*?)["']|.+["'](.*?)["']|["'](.*?)["'])/g;
					 * this.triggerClientEventRe =
					 * /TriggerClientEvent\((\n["'](.*?)["']|\n\s+["'](.*?)["']|.+["'](.*?)["']|["'](.*?)["'])/g;
					 * this.triggerServerEventRe =
					 * /TriggerServerEvent\((\n["'](.*?)["']|\n\s+["'](.*?)["']|.+["'](.*?)["']|["'](.*?)["'])/g;
					 * this.registerNetEventRe =
					 * /RegisterNetEvent\((\n["'](.*?)["']|\n\s+["'](.*?)["']|.+["'](.*?)["']|["'](.*?)["'])/g;
					 * this.esxRegisterServerCallbackRe =
					 * /ESX\.RegisterServerCallback\((\n["'](.*?)["']|\n\s+["'](.*?)["']|.+["'](.*?)["']|["'](.*?)["'])/g;
					 * this.esxTriggerServerCallbackRe =
					 * /ESX\.TriggerServerCallback\((\n["'](.*?)["']|\n\s+["'](.*?)["']|.+["'](.*?)["']|["'](.*?)["'])/g;
					 */
					Pattern eventPatt = Pattern.compile(
							"AddEventHandler\\((\\n[\"'](.*?)[\"']|\\n\\s+[\"'](.*?)[\"']|.+[\"'](.*?)[\"']|[\"'](.*?)[\"'])",
							Pattern.MULTILINE);
					Pattern triggerEventPatt = Pattern.compile(
							"TriggerEvent\\((\\n[\"'](.*?)[\"']|\\n\\s+[\"'](.*?)[\"']|.+[\"'](.*?)[\"']|[\"'](.*?)[\"'])",
							Pattern.MULTILINE);
					Pattern registerServerEventPatt = Pattern.compile(
							"RegisterServerEvent\\((\\n[\"'](.*?)[\"']|\\n\\s+[\"'](.*?)[\"']|.+[\"'](.*?)[\"']|[\"'](.*?)[\"'])",
							Pattern.MULTILINE);
					Pattern triggerClientEventPatt = Pattern.compile(
							"TriggerClientEvent\\((\\n[\"'](.*?)[\"']|\\n\\s+[\"'](.*?)[\"']|.+[\"'](.*?)[\"']|[\"'](.*?)[\"'])",
							Pattern.MULTILINE);
					Pattern triggerServerEventPatt = Pattern.compile(
							"TriggerServerEvent\\((\\n[\"'](.*?)[\"']|\\n\\s+[\"'](.*?)[\"']|.+[\"'](.*?)[\"']|[\"'](.*?)[\"'])",
							Pattern.MULTILINE);
					Pattern registerNetEventPatt = Pattern.compile(
							"RegisterNetEvent\\((\\n[\"'](.*?)[\"']|\\n\\s+[\"'](.*?)[\"']|.+[\"'](.*?)[\"']|[\"'](.*?)[\"'])",
							Pattern.MULTILINE);
					Pattern esxRegisterServerCBPatt = Pattern.compile(
							"ESX\\.RegisterServerCallback\\((\\n[\"'](.*?)[\"']|\\n\\s+[\"'](.*?)[\"']|.+[\"'](.*?)[\"']|[\"'](.*?)[\"'])",
							Pattern.MULTILINE);
					Pattern esxTriggerServerCBPatt = Pattern.compile(
							"ESX\\.TriggerServerCallback\\((\\n[\"'](.*?)[\"']|\\n\\s+[\"'](.*?)[\"']|.+[\"'](.*?)[\"']|[\"'](.*?)[\"'])",
							Pattern.MULTILINE);
					Pattern[] patterns = { eventPatt, triggerClientEventPatt, triggerEventPatt, registerNetEventPatt,
							registerServerEventPatt, triggerServerEventPatt, esxRegisterServerCBPatt,
							esxTriggerServerCBPatt };
					BufferedReader reader = null;
					try {
						reader = new BufferedReader(new FileReader(f.getAbsolutePath()));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
					try {
						StringBuilder stringBuilder = new StringBuilder();
						char[] buffer = new char[1];
						while (reader.read(buffer) != -1) {
							stringBuilder.append(new String(buffer));
							buffer = new char[1];
						}
						reader.close();

						String content = stringBuilder.toString();
						for (Pattern patt : patterns) {
							Matcher match = patt.matcher(content);
							while (match.find()) {
								String eventName = match.group(5);
								if (eventName != null) {
									String newEventName = getRandomEventScramble();
									if (!isFivemEvent(eventName) && !alreadyExists(eventName)) {
										if (!keyVals.containsKey(eventName)) {
											content = content.replaceAll("" + eventName + "", "" + newEventName + "");
											content = content.replaceAll("" + eventName + "", "" + newEventName + "");
											keyVals.put(eventName, newEventName);
											System.out.println("[ResourceEventScrambler] Replaced event named '"
													+ eventName + "' with: " + newEventName);
										} else {
											newEventName = keyVals.get(eventName);
											content = content.replaceAll("" + eventName + "", "" + newEventName + "");
											content = content.replaceAll("" + eventName + "", "" + newEventName + "");
											System.out.println("[ResourceEventScrambler] Replaced event named '"
													+ eventName + "' with: " + keyVals.get(eventName));
										}
									}
								}
							}
						}
						File file = f.getAbsoluteFile();
						FileOutputStream fooStream = new FileOutputStream(file, false); // true to append
						// false to overwrite.
						byte[] bytes = content.getBytes();
						fooStream.write(bytes);
						fooStream.close();
						System.out.println("[ResourceEventScrambler] Rewrote file: " + f.getName());
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}
		for (File f : resources) {
			if (f.getName().toLowerCase().contains(".lua")) {
				// It's a Lua file
				if (!f.getName().contains("__resource")) {
					if (isBlacklistedFile(f)) {
						System.out.println("[ResourceEventScrambler] Skipped Blacklisted File: " + f.getAbsolutePath());
						continue;
					}
					// Replace all the events we have tracked
					BufferedReader reader = null;
					try {
						reader = new BufferedReader(new FileReader(f.getAbsolutePath()));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
					try {
						StringBuilder stringBuilder = new StringBuilder();
						char[] buffer = new char[1];
						while (reader.read(buffer) != -1) {
							stringBuilder.append(new String(buffer));
							buffer = new char[1];
						}
						reader.close();

						String content = stringBuilder.toString();
						for (String eventKey : keyVals.keySet()) {
							content = content.replaceAll("" + eventKey + "", "" + keyVals.get(eventKey) + "");
							content = content.replaceAll("" + eventKey + "", "" + keyVals.get(eventKey) + "");
						}
						File file = f.getAbsoluteFile();
						FileOutputStream fooStream = new FileOutputStream(file, false); // true to append
						// false to overwrite.
						byte[] bytes = content.getBytes();
						fooStream.write(bytes);
						fooStream.close();
						System.out.println("[ResourceEventScrambler] Updated file with latest events: " + f.getName());
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}

		// Let's add the old events to events.lua now
		try {
			String content = "Events = {\n";
			for (String eventName : keyVals.keySet()) {
				content = content + "'" + eventName + "',\n";
			}
			content = content + "}";
			FileOutputStream fooStream = new FileOutputStream(new File("RES_Anticheat/events.lua"), false); // true to
																											// append
			// false to overwrite.
			byte[] bytes = content.getBytes();
			fooStream.write(bytes);
			fooStream.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		System.out.println("[ResourceEventScrambler] SCRAMBLING COMPLETED");
	}

	public static boolean alreadyExists(String val) {
		for (String eventName : keyVals.values()) {
			if (val.equals(eventName)) {
				return true;
			}
		}
		return false;
	}

	private static Random rand = new Random();

	public static String getRandomEventScramble() {
		String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		String newEventName = "";
		boolean found = false;
		while (!found) {
			for (int i = 0; i < 9; i++) {
				newEventName += letters.charAt(rand.nextInt(letters.length()));
			}
			if (!alreadyExists(newEventName)) {
				found = true;
			} else {
				newEventName = "";
			}
		}
		return newEventName;
	}

	public static List<File> listf(String directoryName) {
		File directory = new File(directoryName);

		List<File> resultList = new ArrayList<File>();

		// get all the files from a directory
		File[] fList = directory.listFiles();
		resultList.addAll(Arrays.asList(fList));
		for (File file : fList) {
			if (file.isFile()) {
				// System.out.println(file.getAbsolutePath());
			} else if (file.isDirectory()) {
				if (!isSystemResource(file.getName())) {
					resultList.addAll(listf(file.getAbsolutePath()));
				}
			}
		}
		// System.out.println(fList);
		return resultList;
	}

	public static void copyFolder(File source, File destination) {
		if (source.isDirectory()) {
			if (!destination.exists()) {
				destination.mkdirs();
			}

			String files[] = source.list();

			for (String file : files) {
				File srcFile = new File(source, file);
				File destFile = new File(destination, file);

				copyFolder(srcFile, destFile);
			}
		} else {
			InputStream in = null;
			OutputStream out = null;

			try {
				in = new FileInputStream(source);
				out = new FileOutputStream(destination);

				byte[] buffer = new byte[1024];

				int length;
				while ((length = in.read(buffer)) > 0) {
					out.write(buffer, 0, length);
				}
			} catch (Exception e) {
				try {
					in.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				try {
					out.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
}