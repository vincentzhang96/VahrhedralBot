package co.phoenixlab.discord.commands.dn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.phoenixlab.common.lang.number.ParseInt;
import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.discord.Command;
import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.util.StringUtilities;

public class CalculateCriticalDamage implements Command{

	public static final double CRITDMG_MAX_PERCENT = 1D;
	
    public static final double[] critDmgCaps = {
            2650.0, 3074.0, 3498.0, 3922.0, 4346.0, 4770.0, 5194.0, 5618.0,
            6042.0, 6466.0, 6890.0, 7314.0, 7738.0, 8162.0, 10070.0, 10600.0,
            11130.0, 11660.0, 12190.0, 12720.0, 13250.0, 13780.0, 14310.0,
            14840.0, 15635.0, 16430.0, 17225.0, 18020.0, 18815.0, 19610.0,
            20405.0, 21200.0, 22260.0, 23320.0, 24380.0, 25440.0, 26500.0,
            27560.0, 28620.0, 29680.0, 31641.0, 33575.0, 35510.0, 37206.0,
            39326.0, 41419.0, 43513.0, 45553.0, 47832.0, 50350.0, 55650.0,
            59757.0, 64580.0, 69589.0, 74756.0, 80109.0, 86310.0, 93121.0,
            99375.0, 103350.0, 107987.0, 113950.0, 121237.0, 129850.0, 139787.0,
            151050.0, 163637.0, 177894.0, 193794.0, 211337.0, 228555.0, 245520.0,
            262220.0, 278587.0, 296902.0, 320100.0, 343263.0, 374976.0, 407979.0,
            431970.0, 453390.0, 474810.0, 496230.0, 517650.0, 542640.0, 567630.0,
            592620.0, 617610.0, 642600.0, 671160.0, 769692.0, 801108.0, 832524.0,
            863940.0, 899283.0, 934626.0, 969969.0, 1005312.0, 1040655.0, 1075998.0
    };
	public static final int DEFAULT_LEVEL = 80;
    
    private static final Logger logger = LoggerFactory.getLogger(CalculateCriticalDamage.class);
    private Localizer commandLocalizer;
    
    public CalculateCriticalDamage(){
    	commandLocalizer = StringUtilities.getLocalizer();
    }
    
    @Override
    public void handleCommand(MessageContext context, String args){
    	DiscordApiClient apiClient = context.getApiClient();
    	
    	String msgName = "commands.dn.critdmg.response.invalid";
    	String cmdPrefix = context.getBot().getMainCommandDispatcher().getCommandPrefix();
    	String msg = commandLocalizer.localize(msgName, cmdPrefix);
    	
		args = args.replace(",", ""); //stip commas
		String[] split = args.split(" "); //split the arguments
		
		try{
			int level = DEFAULT_LEVEL;
			switch(split.length){
				case 2:
					level = ParseInt.parseOrDefault(split[1], level);
				case 1:
					String value = split[0];
					if(value.endsWith("%")){
						//Parse the critical damage percentage
						double percent = Double.parseDouble(value.substring(0, value.length() -1));
						percent = Math.min(percent * .01, CRITDMG_MAX_PERCENT);
						
						double critDmg = calcCritDmgReqiredToReachPercent(percent, level);
						
						msgName = "commands.dn.critdmg.response.format.required";
						msg = commandLocalizer.localize(msgName, level, (int)critDmg, (percent + 2D) * 100D);
					}
					else{
						int critDmg = (int) StringUtilities.parseAlphanumeric(value, commandLocalizer);
						double critDmgPercent = calcPercentFromCritDmg(critDmg, level);
						double critDmgCap = critDmgCaps[level - 1];
						
						msgName = "commands.dn.critdmg.response.format";
						msg = commandLocalizer.localize(msgName, level, critDmgPercent,
								(int) (critDmgCap * CRITDMG_MAX_PERCENT),
								(int) (CRITDMG_MAX_PERCENT * 100D) + 200);
					}
			}
		}
		catch(IllegalArgumentException e){
			// NOTE : If(args.isEmpty()) then the bot will return "empty string"
			msg = e.getMessage();
		}
		catch(Exception e){
			logger.warn(e.getMessage(), e);
		}
		finally{
			apiClient.sendMessage(msg, context.getChannel());
		}
    }
	
	private double calcCritDmgReqiredToReachPercent(double percent, int level){
		if(percent < 0){
			throw new IllegalArgumentException("must specify at least 0% critical damage");
		}
		
		if(level < 1 || level > 100){
			String errMsg = commandLocalizer.localize("commands.dn.critdmg.response.level_out_of_range", 1, 100);
			throw new IllegalArgumentException(errMsg);
		}
		
		return critDmgCaps[level - 1] * percent;
	}
	
	private double calcPercentFromCritDmg(int critDmg, int level){
		if(level < 1 || level > 100){
			String errMsg = commandLocalizer.localize("commands.dn.critdmg.response.level_out_of_range", 1, 100);
			throw new IllegalArgumentException(errMsg);
		}
		
        double critDmgPercent;
        double critDmgCap = critDmgCaps[level - 1];
        critDmgPercent = critDmg / critDmgCap;
        critDmgPercent = Math.max(0, Math.min(CRITDMG_MAX_PERCENT, critDmgPercent)) * 100D + 200D;
        return critDmgPercent;
	}
}
