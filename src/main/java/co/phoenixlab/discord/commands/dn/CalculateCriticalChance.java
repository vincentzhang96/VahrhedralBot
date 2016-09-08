package co.phoenixlab.discord.commands.dn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.phoenixlab.common.lang.number.ParseInt;
import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.discord.Command;
import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.util.StringUtilities;

public class CalculateCriticalChance implements Command {
	public static final double[] critCaps = {
			1000.0, 1160.0, 1320.0, 1480.0, 1640.0, 1800.0, 1960.0, 2120.0,
			2280.0, 2440.0, 2600.0, 2760.0, 2920.0, 3080.0, 3800.0, 4000.0,
			4200.0, 4400.0, 4600.0, 4800.0, 5000.0, 5200.0, 5400.0, 5600.0,
			5900.0, 6200.0, 6500.0, 6800.0, 7100.0, 7400.0, 7700.0, 8000.0,
			8400.0, 8800.0, 9200.0, 9600.0, 10000.0, 10400.0, 10800.0, 11200.0,
			12000.0, 12800.0, 13600.0, 14400.0, 15300.0, 16200.0, 17100.0,
			18000.0, 19000.0, 20000.0, 21500.0, 23000.0, 24600.0, 26200.0,
			27900.0, 29600.0, 31400.0, 33200.0, 35200.0, 37200.0, 40200.0,
			43200.0, 46200.0, 49200.0, 52400.0, 55600.0, 58800.0, 62000.0,
			65400.0, 68800.0, 74745.0, 80619.0, 86438.0, 92373.0, 98284.0,
			104245.0, 110176.0, 116008.0, 121899.0, 127685.0, 138684.0, 149565.0,
			160545.0, 171433.0, 182263.0, 192994.0, 203931.0, 214891.0, 225855.0,
			236880.0, 277830.0, 321300.0, 367290.0, 415800.0, 468877.0, 524790.0,
			583537.0, 645120.0, 709537.0, 776790.0
	};
	public static final double CRIT_MAX_PERCENT = 0.89D;
	public static final int DEFAULT_LEVEL = 80;
	
	private static final Logger logger = LoggerFactory.getLogger(CalculateCriticalChance.class);
	
	private Localizer commandLocalizer;
	
	public CalculateCriticalChance() {
		commandLocalizer = StringUtilities.getLocalizer();
	}
	
	@Override
	public void handleCommand(MessageContext context, String args){
		DiscordApiClient apiClient = context.getApiClient();
		
		//pessimistic assumption that input will be wrong.
		String msgName = "commands.dn.crit.response.invalid";
		String cmdPrefix = context.getBot().getMainCommandDispatcher().getCommandPrefix();
		String msg = commandLocalizer.localize(msgName, cmdPrefix);
		
		args = args.replace(",", ""); //stip commas
		String[] split = args.split(" "); //split the arguments

		try{
			int level = DEFAULT_LEVEL;
			switch(split.length){
				case 2:
					//Second argument will always be target level
					level = ParseInt.parseOrDefault(split[1], level);
				case 1:
					String value = split[0];
					if(value.endsWith("%")){
						//Parse the critical percentage
						double percent = Double.parseDouble(value.substring(0, value.length() -1));
						percent = Math.min(percent * .01, CRIT_MAX_PERCENT);
				
						//Calculate the result
						int crit = (int)calcCritRequiredToReachPercent(percent, level);
						
						//Build message for user.
						msgName = "commands.dn.crit.response.format.required";
						msg = commandLocalizer.localize(msgName, level, crit, percent * 100D);
					}
					else{
						//Parse the critical amount
						int crit = (int) StringUtilities.parseAlphanumeric(value, commandLocalizer);
						
						//calculate the result
						double critPercent = calcPercentFromCrit(crit, level);
						
						//Build message for user
						int  critCap = (int) (critCaps[level-1] * CRIT_MAX_PERCENT);
						msgName = "commands.dn.crit.response.format";
						msg = commandLocalizer.localize(msgName, (int)level, (float)critPercent, (int)critCap, (int) (CRIT_MAX_PERCENT * 100D));
					}
					break;
				default : 
					break;
			}
		}	
		catch(IllegalArgumentException e){
			//For NFE we just want to use normal msg
			if(!(e instanceof NumberFormatException)){
				msg = e.getMessage();
			}
		}
		catch(Exception e){
			logger.warn(e.getMessage(), e);
		}
		finally {
			//Send user the response
			apiClient.sendMessage(msg, context.getChannel());
		}
	}
	
	private double calcCritRequiredToReachPercent(double percent, int level){
		if (percent < 0) {
			String errMsg = commandLocalizer.localize("commands.dn.crit.response.low_percent");
			throw new IllegalArgumentException(errMsg);
		}
		
		if (level < 1 || level > 100) {
			String errMsg = commandLocalizer.localize("commands.dn.crit.response.level_out_of_range", 1, 100);
			throw new IllegalArgumentException(errMsg);
		}
		
		return critCaps[level - 1] * percent;
	}
	private double calcPercentFromCrit(int critical, int level){
		if (level < 1 || level > 100) {
			String errMsg = commandLocalizer.localize("commands.dn.crit.response.level_out_of_range", 1, 100);
			throw new IllegalArgumentException(errMsg);
		}
		
		double percent;
		double critCap = critCaps[level-1];
		
		percent = critical / critCap;
		percent = Math.max(0, Math.min(CRIT_MAX_PERCENT, percent)) * 100D;
	
		return percent;
	}
}
