package org.bonitasoft.engine.profile;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ ProfileTest.class,
        ProfileEntryTest.class,
        ProfileMemberTest.class })
public class ProfileTests {

}
