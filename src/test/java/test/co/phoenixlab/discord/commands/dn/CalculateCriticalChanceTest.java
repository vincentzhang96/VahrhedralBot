package test.co.phoenixlab.discord.commands.dn;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;

import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.discord.Command;
import co.phoenixlab.discord.CommandDispatcher;
import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.VahrhedralBot;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Channel;
import co.phoenixlab.discord.api.entities.Message;
import co.phoenixlab.discord.commands.dn.CalculateCriticalChance;
import co.phoenixlab.discord.util.StringUtilities;

public class CalculateCriticalChanceTest {
	private static final String PREFIX = "!";
	MessageContext mockContext;
	
	@Before
	public void setup(){
		//init the mock classes we'll need
		mockContext = mock(MessageContext.class);
		VahrhedralBot mockBot = mock(VahrhedralBot.class);
		CommandDispatcher mockDispatcher = mock(CommandDispatcher.class);
		
		//define the behavior	
		when(mockContext.getApiClient()).thenReturn(new MockClient());
		when(mockDispatcher.getCommandPrefix()).thenReturn(PREFIX);
		when(mockContext.getBot()).thenReturn(mockBot);
		when(mockBot.getMainCommandDispatcher()).thenReturn(mockDispatcher);
		
	}
	

	@Test
	public void handleCommandEmptyArgTest() {
		//Init
		String argument = "";
		Command command = new CalculateCriticalChance();
		
		//Code being tested
		command.handleCommand(getMockContext(), argument);
		
		//Check results of test
		MockClient client = (MockClient)getMockContext().getApiClient();
		String result = client.getMsgSent();
		
		Localizer loc = StringUtilities.getLocalizer();
		String msgName = "commands.dn.crit.response.invalid";
		String expect = loc.localize(msgName, PREFIX);
		
		assertEquals(expect, result);
	}
	
	
	@Test
	public void handleCommand2ArgumentPercentage() {
		//Init
		String argument = "19% 85";
		Command command = new CalculateCriticalChance();
		
		//Code being tested
		command.handleCommand(getMockContext(), argument);
		
		//Check results of test
		MockClient client = (MockClient)mockContext.getApiClient();
		String result = client.getMsgSent();
		
		Localizer loc = StringUtilities.getLocalizer();
		String msgName = "commands.dn.crit.response.format.required";
		String expect = loc.localize(msgName,
				85,
				34629,
				19D);
		assertEquals(expect, result);
	}

	@Test
	public void handleCommand2ArgumentConstant() {
		//Init
		String argument = "140000 85";
		Command command = new CalculateCriticalChance();
		
		//Code being tested
		command.handleCommand(getMockContext(), argument);
		
		//Check results of test
		MockClient client = (MockClient)mockContext.getApiClient();
		String result = client.getMsgSent();
		
		Localizer loc = StringUtilities.getLocalizer();
		String msgName = "commands.dn.crit.response.format";
		String expect = loc.localize(msgName,
				85,
				76.8D,
				162214,
				89);
		assertEquals(expect, result);
	}

	@Test
	public void handleCommand2ArgumentConstantOutOfRangeLevels() {
		//Init
		String argument = "140000 101";
		Command command = new CalculateCriticalChance();
		
		//Code being tested
		command.handleCommand(getMockContext(), argument);
		
		//Check results of test
		MockClient client = (MockClient)mockContext.getApiClient();
		String result = client.getMsgSent();
		
		Localizer loc = StringUtilities.getLocalizer();
		String msgName = "commands.dn.crit.response.level_out_of_range";
		String expect = loc.localize(msgName,
				1, 100);
		assertEquals(expect, result);
	}
	
	@Test
	public void handleCommand2ArgumentPercentageOutOfRangeLevels() {
		//Init
		String argument = "19% 101";
		Command command = new CalculateCriticalChance();
		
		//Code being tested
		command.handleCommand(getMockContext(), argument);
		
		//Check results of test
		MockClient client = (MockClient)mockContext.getApiClient();
		String result = client.getMsgSent();
		
		Localizer loc = StringUtilities.getLocalizer();
		String expect = loc
				.localize("commands.dn.crit.response.level_out_of_range",
				1,
				100);
		assertEquals(expect, result);
	}
	

	@Test
	public void handleCommand2ArgumentPercentageOutOfRangeCritDmg() {
		//Init
		String argument = "-1% 90";
		Command command = new CalculateCriticalChance();
		
		//Code being tested
		command.handleCommand(getMockContext(), argument);
		
		//Check results of test
		MockClient client = (MockClient)mockContext.getApiClient();
		String result = client.getMsgSent();
		
		Localizer loc = StringUtilities.getLocalizer();
		String expect = loc.localize("commands.dn.crit.response.low_percent");
		assertEquals(expect, result);
	}
	
	MessageContext getMockContext(){
		return mockContext;
	}
	
	public static class MockClient extends DiscordApiClient{
		String msgSent;
		
		@Override
		public Future<Message> sendMessage(String body, Channel channel) {
			msgSent = body;
			return null;
		}
		
		public String getMsgSent(){
			return msgSent;
		}
	}
}
