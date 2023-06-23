package r3z

import r3z.system.misc.testLogger
import r3z.server.types.ServerObjects
import r3z.system.FakeSystemConfigurationPersistence
import r3z.uitests.Drivers

val webDriver = Drivers.CHROME

val fakeServerObjects = ServerObjects(emptyMap(), testLogger, 0, 0, false, scp = FakeSystemConfigurationPersistence(), socketTimeout = 10*1000)