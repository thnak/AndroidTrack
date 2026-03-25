package com.androidtrack.app.presentation.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import com.androidtrack.app.data.model.BrokerConfig
import com.androidtrack.app.data.model.DeviceConfig
import com.androidtrack.app.data.model.DiPin
import com.androidtrack.app.data.model.PinMode
import com.androidtrack.app.data.repository.ConfigRepository
import com.androidtrack.app.domain.usecase.ManagePinsUseCase
import com.androidtrack.app.domain.usecase.SaveBrokerConfigUseCase
import com.androidtrack.app.domain.usecase.SaveDeviceConfigUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private val testAndroidId = "test-android-id-1234"

    // Dependencies
    private lateinit var configRepository: ConfigRepository
    private lateinit var saveBrokerConfigUseCase: SaveBrokerConfigUseCase
    private lateinit var saveDeviceConfigUseCase: SaveDeviceConfigUseCase
    private lateinit var managePinsUseCase: ManagePinsUseCase
    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver

    // static mock for Settings.Secure
    private lateinit var mockedSettingsSecure: MockedStatic<Settings.Secure>

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        configRepository = mock()
        saveBrokerConfigUseCase = mock()
        saveDeviceConfigUseCase = mock()
        managePinsUseCase = mock()
        context = mock()
        contentResolver = mock()

        whenever(context.contentResolver).thenReturn(contentResolver)
        whenever(managePinsUseCase.observeAll()).thenReturn(flowOf(emptyList()))
        whenever(configRepository.observeBrokerConfig()).thenReturn(flowOf(null))
        whenever(configRepository.observeDeviceConfig()).thenReturn(flowOf(null))

        // Mock static Settings.Secure.getString to return testAndroidId.
        mockedSettingsSecure = Mockito.mockStatic(Settings.Secure::class.java)
        mockedSettingsSecure.`when`<String> {
            Settings.Secure.getString(any(), any())
        }.thenReturn(testAndroidId)

        viewModel = SettingsViewModel(
            configRepository,
            saveBrokerConfigUseCase,
            saveDeviceConfigUseCase,
            managePinsUseCase,
            context
        )
    }

    @After
    fun tearDown() {
        mockedSettingsSecure.close()
        Dispatchers.resetMain()
    }

    // --- deviceClientId -----------------------------------------------------------

    @Test
    fun `deviceClientId is resolved from ANDROID_ID`() {
        assertEquals(testAndroidId, viewModel.deviceClientId)
    }

    // --- initial form state -------------------------------------------------------

    @Test
    fun `initial brokerForm has default values when no config is stored`() {
        val form = viewModel.brokerForm.value
        assertEquals("broker.hivemq.com", form.host)
        assertEquals("1883", form.port)
        assertEquals("", form.username)
        assertEquals("", form.password)
        assertFalse(form.secure)
    }

    @Test
    fun `initial deviceForm has default values when no config is stored`() {
        val form = viewModel.deviceForm.value
        assertEquals("DEV-001", form.deviceId)
        assertEquals("GATEWAY-V3", form.deviceType)
    }

    // --- broker form field updates ------------------------------------------------

    @Test
    fun `updateBrokerHost changes host in brokerForm`() {
        viewModel.updateBrokerHost("new.broker.com")
        assertEquals("new.broker.com", viewModel.brokerForm.value.host)
    }

    @Test
    fun `updateBrokerPort changes port in brokerForm`() {
        viewModel.updateBrokerPort("9999")
        assertEquals("9999", viewModel.brokerForm.value.port)
    }

    @Test
    fun `updateBrokerUsername changes username in brokerForm`() {
        viewModel.updateBrokerUsername("admin")
        assertEquals("admin", viewModel.brokerForm.value.username)
    }

    @Test
    fun `updateBrokerPassword changes password in brokerForm`() {
        viewModel.updateBrokerPassword("secret")
        assertEquals("secret", viewModel.brokerForm.value.password)
    }

    // --- secure toggle port auto-switch -------------------------------------------

    @Test
    fun `enabling secure switches port from 1883 to 8883`() {
        viewModel.updateBrokerPort("1883")
        viewModel.updateBrokerSecure(true)
        assertEquals("8883", viewModel.brokerForm.value.port)
        assertTrue(viewModel.brokerForm.value.secure)
    }

    @Test
    fun `disabling secure switches port from 8883 to 1883`() {
        viewModel.updateBrokerPort("8883")
        viewModel.updateBrokerSecure(false)
        assertEquals("1883", viewModel.brokerForm.value.port)
        assertFalse(viewModel.brokerForm.value.secure)
    }

    @Test
    fun `enabling secure does not change a custom port`() {
        viewModel.updateBrokerPort("5555")
        viewModel.updateBrokerSecure(true)
        assertEquals("5555", viewModel.brokerForm.value.port)
    }

    @Test
    fun `disabling secure does not change a custom port`() {
        viewModel.updateBrokerPort("7777")
        viewModel.updateBrokerSecure(false)
        assertEquals("7777", viewModel.brokerForm.value.port)
    }

    // --- device form field updates ------------------------------------------------

    @Test
    fun `updateDeviceId changes deviceId in deviceForm`() {
        viewModel.updateDeviceId("DEV-999")
        assertEquals("DEV-999", viewModel.deviceForm.value.deviceId)
    }

    @Test
    fun `updateDeviceType changes deviceType in deviceForm`() {
        viewModel.updateDeviceType("SENSOR-V2")
        assertEquals("SENSOR-V2", viewModel.deviceForm.value.deviceType)
    }

    // --- saveBrokerConfig ---------------------------------------------------------

    @Test
    fun `saveBrokerConfig calls use case with current form values`() = runTest {
        viewModel.updateBrokerHost("host.test.com")
        viewModel.updateBrokerPort("1883")
        viewModel.updateBrokerSecure(false)
        whenever(
            saveBrokerConfigUseCase.invoke(
                BrokerConfig(
                    host = "host.test.com",
                    port = 1883,
                    username = "",
                    password = "",
                    clientId = testAndroidId,
                    secure = false
                )
            )
        ).thenReturn(Result.success(Unit))

        viewModel.saveBrokerConfig()
        runCurrent()

        verify(saveBrokerConfigUseCase).invoke(
            BrokerConfig(
                host = "host.test.com",
                port = 1883,
                username = "",
                password = "",
                clientId = testAndroidId,
                secure = false
            )
        )
    }

    // --- saveDeviceConfig ---------------------------------------------------------

    @Test
    fun `saveDeviceConfig calls use case with current form values`() = runTest {
        viewModel.updateDeviceId("MYDEV")
        viewModel.updateDeviceType("TYPE-A")
        whenever(
            saveDeviceConfigUseCase.invoke(DeviceConfig(deviceId = "MYDEV", deviceType = "TYPE-A"))
        ).thenReturn(Result.success(Unit))

        viewModel.saveDeviceConfig()
        runCurrent()

        verify(saveDeviceConfigUseCase).invoke(DeviceConfig(deviceId = "MYDEV", deviceType = "TYPE-A"))
    }

    // --- pin dialog open / close --------------------------------------------------

    @Test
    fun `showAddPinDialog opens dialog in add mode`() {
        viewModel.showAddPinDialog()
        val dialog = viewModel.pinDialog.value
        assertTrue(dialog.isVisible)
        assertFalse(dialog.isEditing)
        assertEquals("", dialog.pinNumber)
    }

    @Test
    fun `showEditPinDialog opens dialog in edit mode with pin data`() {
        val pin = DiPin(id = 7, pinNumber = "03", mode = PinMode.AUTO, timerMs = 2000L, pulseTime = 500L)
        viewModel.showEditPinDialog(pin)
        val dialog = viewModel.pinDialog.value
        assertTrue(dialog.isVisible)
        assertTrue(dialog.isEditing)
        assertEquals(7, dialog.editId)
        assertEquals("03", dialog.pinNumber)
        assertEquals(PinMode.AUTO, dialog.mode)
        assertEquals("2000", dialog.timerMs)
        assertEquals("500", dialog.pulseTime)
    }

    @Test
    fun `dismissPinDialog hides the dialog and clears state`() {
        viewModel.showAddPinDialog()
        viewModel.dismissPinDialog()
        val dialog = viewModel.pinDialog.value
        assertFalse(dialog.isVisible)
        assertFalse(dialog.isEditing)
        assertEquals("", dialog.pinNumber)
    }

    // --- pin dialog field updates -------------------------------------------------

    @Test
    fun `updatePinNumber changes pinNumber and clears error`() {
        viewModel.showAddPinDialog()
        // Simulate an error first.
        viewModel.updatePinNumber("bad")
        // Then change to a valid number.
        viewModel.updatePinNumber("42")
        val dialog = viewModel.pinDialog.value
        assertEquals("42", dialog.pinNumber)
        assertNull(dialog.pinNumberError)
    }

    @Test
    fun `updatePinMode changes mode in dialog`() {
        viewModel.showAddPinDialog()
        viewModel.updatePinMode(PinMode.AUTO)
        assertEquals(PinMode.AUTO, viewModel.pinDialog.value.mode)
    }

    @Test
    fun `updatePinTimer changes timerMs in dialog`() {
        viewModel.showAddPinDialog()
        viewModel.updatePinTimer("9000")
        assertEquals("9000", viewModel.pinDialog.value.timerMs)
    }

    @Test
    fun `updatePinPulse changes pulseTime in dialog`() {
        viewModel.showAddPinDialog()
        viewModel.updatePinPulse("250")
        assertEquals("250", viewModel.pinDialog.value.pulseTime)
    }

    // --- savePinFromDialog --------------------------------------------------------

    @Test
    fun `savePinFromDialog success closes the dialog`() = runTest {
        viewModel.showAddPinDialog()
        viewModel.updatePinNumber("01")
        whenever(managePinsUseCase.addPin(any())).thenReturn(Result.success(Unit))

        viewModel.savePinFromDialog()
        runCurrent()

        assertFalse(viewModel.pinDialog.value.isVisible)
    }

    @Test
    fun `savePinFromDialog failure keeps dialog open and sets pinNumberError`() = runTest {
        viewModel.showAddPinDialog()
        viewModel.updatePinNumber("")
        whenever(managePinsUseCase.addPin(any()))
            .thenReturn(Result.failure(IllegalArgumentException("Pin number cannot be empty")))

        viewModel.savePinFromDialog()
        runCurrent()

        val dialog = viewModel.pinDialog.value
        assertTrue(dialog.isVisible)
        assertEquals("Pin number cannot be empty", dialog.pinNumberError)
    }

    @Test
    fun `savePinFromDialog in edit mode calls updatePin`() = runTest {
        val existing = DiPin(id = 5, pinNumber = "05", mode = PinMode.MANUAL)
        viewModel.showEditPinDialog(existing)
        whenever(managePinsUseCase.updatePin(any())).thenReturn(Result.success(Unit))

        viewModel.savePinFromDialog()
        runCurrent()

        verify(managePinsUseCase).updatePin(any())
    }

    // --- deletePin ----------------------------------------------------------------

    @Test
    fun `deletePin calls managePinsUseCase deletePin with correct id`() = runTest {
        viewModel.deletePin(id = 99)
        runCurrent()
        verify(managePinsUseCase).deletePin(99)
    }

    // --- snackbar -----------------------------------------------------------------

    @Test
    fun `clearSnackbar nullifies snackbarMessage`() = runTest {
        // Trigger a snackbar message via saveDeviceConfig success.
        whenever(saveDeviceConfigUseCase.invoke(any())).thenReturn(Result.success(Unit))
        whenever(context.getString(any())).thenReturn("Device settings saved")

        viewModel.saveDeviceConfig()
        runCurrent()

        viewModel.clearSnackbar()
        assertNull(viewModel.snackbarMessage.value)
    }

    // --- config loaded from DB flows ----------------------------------------------

    @Test
    fun `brokerForm updates when observeBrokerConfig emits a config`() = runTest {
        val savedConfig = BrokerConfig(host = "saved.host.com", port = 8883, secure = true)
        whenever(configRepository.observeBrokerConfig()).thenReturn(flowOf(savedConfig))
        whenever(configRepository.observeDeviceConfig()).thenReturn(flowOf(null))

        val vm = SettingsViewModel(
            configRepository,
            saveBrokerConfigUseCase,
            saveDeviceConfigUseCase,
            managePinsUseCase,
            context
        )
        runCurrent()

        val form = vm.brokerForm.value
        assertEquals("saved.host.com", form.host)
        assertEquals("8883", form.port)
        assertTrue(form.secure)
    }

    @Test
    fun `deviceForm updates when observeDeviceConfig emits a config`() = runTest {
        whenever(configRepository.observeBrokerConfig()).thenReturn(flowOf(null))
        whenever(configRepository.observeDeviceConfig()).thenReturn(
            flowOf(DeviceConfig(deviceId = "DB-DEV", deviceType = "DB-TYPE"))
        )

        val vm = SettingsViewModel(
            configRepository,
            saveBrokerConfigUseCase,
            saveDeviceConfigUseCase,
            managePinsUseCase,
            context
        )
        runCurrent()

        val form = vm.deviceForm.value
        assertEquals("DB-DEV", form.deviceId)
        assertEquals("DB-TYPE", form.deviceType)
    }
}
