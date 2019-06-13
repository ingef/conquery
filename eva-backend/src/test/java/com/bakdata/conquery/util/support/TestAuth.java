package com.bakdata.conquery.util.support;

import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;

public class TestAuth {
	public final static String SECRET = "TEST_SECRET!!!!!";
	public static class SuperMandator {
		public final static Mandator INSTANCE = new Mandator("999999998","superMandator");
	}
	public static class SuperUser{
		// sp_portal_username=superUser&sp_email=superUser&sp_kasse_name_0=Demokasse&sp_kasse_ik_0=999999998&sp_timestamp=2017-06-25T15%3a12%3a57.595
		public final static String TOKEN = "Rt%2BER%2FDkGKNjgivRMegd%2B06Y00jsKruaW2UbBCC1USq7cpx%2B8n%2Bhi%2F%2FvCixdqZp%2Fzj182LVtN728Z%2Bwj2PkRMxWREPnv6NKQY1puMGWHKuhXEyP6vnQVevxQFmMwKmniJvlJSeSccl1cWzJRwM%2Fkt1Q%2FzIU0gd0VF2Hymlj0eHDOAYZi7%2F7%2BOQHBKCt7snz6";
		public final static String EMAIL = "superUser";
		public final static User INSTANCE = new User(EMAIL, "superUserName");
	}
	
	public static class TestMandator {
		public final static Mandator INSTANCE = new Mandator("999999999", "TestMandator");
		
	}
	public static class TestUserUnknown{
		// [sp_portal_username=Test, Tester, sp_email=test@test.de, sp_kasse_name_0=Demokasse, sp_kasse_ik_0=999999999, sp_timestamp=2017-06-25T15:12:57.595]
		public final static String TOKEN = "Rt%2BER%2FDkGKNjgivRMegd%2B06Y00jsKruaW2UbBCC1USqz0697zaz%2BRlhALl2QJLbj1exyPNd2LFprir%2B5rE%2BTKuxWhI6v0oFLP1UNc5B53e13jfZg7K3Y2v1DdeFs2e602ywk6WxZzlwOzbJEJz42V8WPq9Bs2EoimLptmnzLGbW%2BlYRcIKGrZ1cz4cU5Vtdbh2j5fhqqipSn6W5Byku1DpXcBSQmV3Z34elMJRXiQ7%2BcUKVLnwohBpuXPLrJ0RdH";
		public final static String EMAIL = "testunkown@TestMandatorUnkown.de";
		public final static User INSTANCE = new User(EMAIL, "TestUserUnknown");
	}
	public static class TestUser{
		// [sp_portal_username=Test, Tester, sp_email=test@test.de, sp_kasse_name_0=Demokasse, sp_kasse_ik_0=999999999, sp_timestamp=2017-06-25T15:12:57.595]
		public final static String TOKEN = "Rt%2BER%2FDkGKNjgivRMegd%2B%2F0cqeMGgAYSI8pqrjqVJ5CMTBy5zBRZKqP5Sk1ZfwSnP71NbbyM4rhgtITMv5esCer00z3yuwcA39fyonIAitq8nX3IK7X04V56BwUJnbblSQCbWO5NWZBpu5CNs%2FxnA4do%2BX4aqoqUp%2BluQcpLtQ6V3AUkJld2d%2BHpTCUV4kO%2FnFClS58KIQablzy6ydEXRw%3D%3D";
		public final static String EMAIL = "test@test.de";
		public final static User INSTANCE = new User(EMAIL, "testUserName");
	}
}
