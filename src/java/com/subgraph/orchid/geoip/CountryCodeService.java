package com.subgraph.orchid.geoip;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import com.subgraph.orchid.data.IPv4Address;

import net.i2p.I2PAppContext;

public class CountryCodeService {
	private final static Logger logger = Logger.getLogger(CountryCodeService.class.getName());
	private final static String DATABASE_FILENAME = "GeoIP.dat";
	private final static int COUNTRY_BEGIN = 16776960;
	private final static int STANDARD_RECORD_LENGTH = 3;
	private final static int MAX_RECORD_LENGTH = 4;
	private final static CountryCodeService DEFAULT_INSTANCE = new CountryCodeService();
	
	public static CountryCodeService getInstance() {
		return DEFAULT_INSTANCE;
	}
	
	private static final String[] COUNTRY_CODES = { "--", "AP", "EU", "AD", "AE",
		"AF", "AG", "AI", "AL", "AM", "CW", "AO", "AQ", "AR", "AS", "AT",
		"AU", "AW", "AZ", "BA", "BB", "BD", "BE", "BF", "BG", "BH", "BI",
		"BJ", "BM", "BN", "BO", "BR", "BS", "BT", "BV", "BW", "BY", "BZ",
		"CA", "CC", "CD", "CF", "CG", "CH", "CI", "CK", "CL", "CM", "CN",
		"CO", "CR", "CU", "CV", "CX", "CY", "CZ", "DE", "DJ", "DK", "DM",
		"DO", "DZ", "EC", "EE", "EG", "EH", "ER", "ES", "ET", "FI", "FJ",
		"FK", "FM", "FO", "FR", "SX", "GA", "GB", "GD", "GE", "GF", "GH",
		"GI", "GL", "GM", "GN", "GP", "GQ", "GR", "GS", "GT", "GU", "GW",
		"GY", "HK", "HM", "HN", "HR", "HT", "HU", "ID", "IE", "IL", "IN",
		"IO", "IQ", "IR", "IS", "IT", "JM", "JO", "JP", "KE", "KG", "KH",
		"KI", "KM", "KN", "KP", "KR", "KW", "KY", "KZ", "LA", "LB", "LC",
		"LI", "LK", "LR", "LS", "LT", "LU", "LV", "LY", "MA", "MC", "MD",
		"MG", "MH", "MK", "ML", "MM", "MN", "MO", "MP", "MQ", "MR", "MS",
		"MT", "MU", "MV", "MW", "MX", "MY", "MZ", "NA", "NC", "NE", "NF",
		"NG", "NI", "NL", "NO", "NP", "NR", "NU", "NZ", "OM", "PA", "PE",
		"PF", "PG", "PH", "PK", "PL", "PM", "PN", "PR", "PS", "PT", "PW",
		"PY", "QA", "RE", "RO", "RU", "RW", "SA", "SB", "SC", "SD", "SE",
		"SG", "SH", "SI", "SJ", "SK", "SL", "SM", "SN", "SO", "SR", "ST",
		"SV", "SY", "SZ", "TC", "TD", "TF", "TG", "TH", "TJ", "TK", "TM",
		"TN", "TO", "TL", "TR", "TT", "TV", "TW", "TZ", "UA", "UG", "UM",
		"US", "UY", "UZ", "VA", "VC", "VE", "VG", "VI", "VN", "VU", "WF",
		"WS", "YE", "YT", "RS", "ZA", "ZM", "ME", "ZW", "A1", "A2", "O1",
		"AX", "GG", "IM", "JE", "BL", "MF", "BQ", "SS", "O1" };
	
	private static final String[] COUNTRY_NAMES = { "N/A", "Asia/Pacific Region",
		"Europe", "Andorra", "United Arab Emirates", "Afghanistan",
		"Antigua and Barbuda", "Anguilla", "Albania", "Armenia", "Curacao",
		"Angola", "Antarctica", "Argentina", "American Samoa", "Austria",
		"Australia", "Aruba", "Azerbaijan", "Bosnia and Herzegovina",
		"Barbados", "Bangladesh", "Belgium", "Burkina Faso", "Bulgaria",
		"Bahrain", "Burundi", "Benin", "Bermuda", "Brunei Darussalam",
		"Bolivia", "Brazil", "Bahamas", "Bhutan", "Bouvet Island",
		"Botswana", "Belarus", "Belize", "Canada",
		"Cocos (Keeling) Islands", "Congo, The Democratic Republic of the",
		"Central African Republic", "Congo", "Switzerland",
		"Cote D'Ivoire", "Cook Islands", "Chile", "Cameroon", "China",
		"Colombia", "Costa Rica", "Cuba", "Cape Verde", "Christmas Island",
		"Cyprus", "Czech Republic", "Germany", "Djibouti", "Denmark",
		"Dominica", "Dominican Republic", "Algeria", "Ecuador", "Estonia",
		"Egypt", "Western Sahara", "Eritrea", "Spain", "Ethiopia",
		"Finland", "Fiji", "Falkland Islands (Malvinas)",
		"Micronesia, Federated States of", "Faroe Islands", "France",
		"Sint Maarten (Dutch part)", "Gabon", "United Kingdom", "Grenada",
		"Georgia", "French Guiana", "Ghana", "Gibraltar", "Greenland",
		"Gambia", "Guinea", "Guadeloupe", "Equatorial Guinea", "Greece",
		"South Georgia and the South Sandwich Islands", "Guatemala",
		"Guam", "Guinea-Bissau", "Guyana", "Hong Kong",
		"Heard Island and McDonald Islands", "Honduras", "Croatia",
		"Haiti", "Hungary", "Indonesia", "Ireland", "Israel", "India",
		"British Indian Ocean Territory", "Iraq",
		"Iran, Islamic Republic of", "Iceland", "Italy", "Jamaica",
		"Jordan", "Japan", "Kenya", "Kyrgyzstan", "Cambodia", "Kiribati",
		"Comoros", "Saint Kitts and Nevis",
		"Korea, Democratic People's Republic of", "Korea, Republic of",
		"Kuwait", "Cayman Islands", "Kazakhstan",
		"Lao People's Democratic Republic", "Lebanon", "Saint Lucia",
		"Liechtenstein", "Sri Lanka", "Liberia", "Lesotho", "Lithuania",
		"Luxembourg", "Latvia", "Libya", "Morocco", "Monaco",
		"Moldova, Republic of", "Madagascar", "Marshall Islands",
		"Macedonia", "Mali", "Myanmar", "Mongolia", "Macau",
		"Northern Mariana Islands", "Martinique", "Mauritania",
		"Montserrat", "Malta", "Mauritius", "Maldives", "Malawi", "Mexico",
		"Malaysia", "Mozambique", "Namibia", "New Caledonia", "Niger",
		"Norfolk Island", "Nigeria", "Nicaragua", "Netherlands", "Norway",
		"Nepal", "Nauru", "Niue", "New Zealand", "Oman", "Panama", "Peru",
		"French Polynesia", "Papua New Guinea", "Philippines", "Pakistan",
		"Poland", "Saint Pierre and Miquelon", "Pitcairn Islands",
		"Puerto Rico", "Palestinian Territory", "Portugal", "Palau",
		"Paraguay", "Qatar", "Reunion", "Romania", "Russian Federation",
		"Rwanda", "Saudi Arabia", "Solomon Islands", "Seychelles", "Sudan",
		"Sweden", "Singapore", "Saint Helena", "Slovenia",
		"Svalbard and Jan Mayen", "Slovakia", "Sierra Leone", "San Marino",
		"Senegal", "Somalia", "Suriname", "Sao Tome and Principe",
		"El Salvador", "Syrian Arab Republic", "Swaziland",
		"Turks and Caicos Islands", "Chad", "French Southern Territories",
		"Togo", "Thailand", "Tajikistan", "Tokelau", "Turkmenistan",
		"Tunisia", "Tonga", "Timor-Leste", "Turkey", "Trinidad and Tobago",
		"Tuvalu", "Taiwan", "Tanzania, United Republic of", "Ukraine",
		"Uganda", "United States Minor Outlying Islands", "United States",
		"Uruguay", "Uzbekistan", "Holy See (Vatican City State)",
		"Saint Vincent and the Grenadines", "Venezuela",
		"Virgin Islands, British", "Virgin Islands, U.S.", "Vietnam",
		"Vanuatu", "Wallis and Futuna", "Samoa", "Yemen", "Mayotte",
		"Serbia", "South Africa", "Zambia", "Montenegro", "Zimbabwe",
		"Anonymous Proxy", "Satellite Provider", "Other", "Aland Islands",
		"Guernsey", "Isle of Man", "Jersey", "Saint Barthelemy",
		"Saint Martin", "Bonaire, Saint Eustatius and Saba", "South Sudan",
		"Other" };

	private final byte[] database;

	public CountryCodeService() {
		this.database = loadDatabase();
	}
	
	private static byte[] loadDatabase() {
		final InputStream input = openDatabaseStream();
		final File dataDir = new File(I2PAppContext.getGlobalContext().getConfigDir(), "plugins/orchid/geoip");
		final File dbFile = new File(dataDir, DATABASE_FILENAME);
		if(input == null) {
			logger.warning("Failed to open '" + DATABASE_FILENAME + "' database file in " + dataDir + " for country code lookups");
			return null;
		} else {
			logger.info("Loaded '" + dataDir + "/" + DATABASE_FILENAME + "' for country code lookups");
		}
		try {
			return loadEntireStream(input);
		} catch (IOException e) {
			logger.warning("IO error reading database file for country code lookups");
			return null;
		} finally {
			try {
				input.close();
			} catch (IOException e) { }
		}
	}

	private static InputStream openDatabaseStream() {
		final InputStream input = tryResourceOpen();
		if(input != null) {
			return input;
		} else {
			return tryFilesystemOpen();
		}
	}

	private static InputStream tryFilesystemOpen() {
		final File dataDir = new File(I2PAppContext.getGlobalContext().getConfigDir(), "plugins/orchid/geoip");
		final File dbFile = new File(dataDir, DATABASE_FILENAME);
		if(!dbFile.canRead()) {
			return null;
		}
		try {
			return new FileInputStream(dbFile);
		} catch (FileNotFoundException e) {
			return null;
		}
	}
	
	private static InputStream tryResourceOpen() {
		final File dataDir = new File(I2PAppContext.getGlobalContext().getConfigDir(), "plugins/orchid/geoip");
		return CountryCodeService.class.getResourceAsStream(dataDir + "/" + DATABASE_FILENAME);
	}

	private static byte[] loadEntireStream(InputStream input) throws IOException {
		final ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
		copy(input, output);
		return output.toByteArray();
	}
	
	private static int copy(InputStream input, OutputStream output) throws IOException {
		final byte[] buffer = new byte[4096];
		int count = 0;
		int n = 0;
		while((n = input.read(buffer)) != -1) {
			output.write(buffer, 0, n);
			count += n;
		}
		return count;
	}
	
	public String getCountryCodeForAddress(IPv4Address address) {
		return COUNTRY_CODES[seekCountry(address)];
	}

	public String getCountryNameForAddress(IPv4Address address) {
		return COUNTRY_NAMES[seekCountry(address)];
	}

	private int seekCountry(IPv4Address address) {
		if(database == null) {
			return 0;
		}
		
		final byte[] record = new byte[2 * MAX_RECORD_LENGTH];
		final int[] x = new int[2];
		final long ip = address.getAddressData() & 0xFFFFFFFFL;
		
		int offset = 0;
		for(int depth = 31; depth >= 0; depth--) {
			loadRecord(offset, record);

			x[0] = unpackRecordValue(record, 0);
			x[1] = unpackRecordValue(record, 1);
			
			int xx = ((ip & (1 << depth)) > 0) ? (x[1]) : (x[0]);
			
			if(xx >= COUNTRY_BEGIN) {
				final int idx = xx - COUNTRY_BEGIN;
				if(idx < 0 || idx > COUNTRY_CODES.length) {
					logger.warning("Invalid index calculated looking up country code record for: [" + address + "] idx = " + idx);
					return 0;
				} else {
					return idx;
				}
			} else {
				offset = xx;
			}
			
		}
		logger.warning("No country code record found for: " + address);
		return 0;
	}

	private void loadRecord(int offset, byte[] recordBuffer) {
		final int dbOffset = 2 * STANDARD_RECORD_LENGTH * offset;
		System.arraycopy(database, dbOffset, recordBuffer, 0, recordBuffer.length);
	}

	private int unpackRecordValue(byte[] record, int idx) {
		final int valueOffset = idx * STANDARD_RECORD_LENGTH;
		int value = 0;
		for(int i = 0; i < STANDARD_RECORD_LENGTH; i++) {
			int octet = record[valueOffset + i] & 0xFF;
			value += (octet << (i * 8));
		}
		return value;
	}

}
