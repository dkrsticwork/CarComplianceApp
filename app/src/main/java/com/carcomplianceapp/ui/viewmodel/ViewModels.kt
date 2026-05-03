package com.carcomplianceapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carcomplianceapp.data.local.PreferencesManager
import com.carcomplianceapp.data.remote.AiApiService
import com.carcomplianceapp.data.repository.CarRepository
import com.carcomplianceapp.data.repository.TaskRepository
import com.carcomplianceapp.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ── ApiKeyViewModel ───────────────────────────────────────────────────────────

data class ApiKeyUiState(
    val rawKey: String = "",
    val detectedProvider: AiProvider? = null,
    val isSaving: Boolean = false
)

@HiltViewModel
class ApiKeyViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val aiApiService: AiApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApiKeyUiState())
    val uiState: StateFlow<ApiKeyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.apiKeyConfig.firstOrNull()?.let { config ->
                _uiState.update { it.copy(rawKey = config.rawKey, detectedProvider = config.provider) }
            }
        }
    }

    fun onKeyChanged(key: String) {
        val provider = if (key.isNotBlank()) {
            runCatching { aiApiService.detectProvider(key) }.getOrNull()
        } else null
        _uiState.update { it.copy(rawKey = key, detectedProvider = provider) }
    }

    fun saveKey(onDone: () -> Unit) {
        val state = _uiState.value
        if (state.rawKey.isBlank()) return
        val provider = state.detectedProvider ?: aiApiService.detectProvider(state.rawKey)
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            preferencesManager.saveApiKey(state.rawKey, provider)
            _uiState.update { it.copy(isSaving = false) }
            onDone()
        }
    }
}

// ── AddCarViewModel ───────────────────────────────────────────────────────────

data class AddCarUiState(
    val countryCode: String = "RS",
    val countryDisplay: String = "Serbia 🇷🇸",
    val makeQuery: String = "",
    val filteredMakes: List<String> = CAR_MAKES.take(8),
    val selectedMake: String = "",
    val availableModels: List<String> = emptyList(),
    val selectedModel: String = "",
    val selectedYear: Int? = null,
    val selectedFuel: FuelType = FuelType.PETROL,
    val lastServiceMonth: String = "",
    val insuranceExpiry: String = "",
    val registrationExpiry: String = "",
    val odometerKm: String = "",
    val documentPaths: List<String> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AddCarViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val taskRepository: TaskRepository,
    private val preferencesManager: PreferencesManager,
    private val aiApiService: AiApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddCarUiState())
    val uiState: StateFlow<AddCarUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadCar(carId: Long) {
        viewModelScope.launch {
            carRepository.getCarById(carId)?.let { car ->
                _uiState.update { s -> s.copy(
                    countryCode = car.countryCode,
                    selectedMake = car.make,
                    makeQuery = car.make,
                    availableModels = CAR_MODELS[car.make] ?: DEFAULT_MODELS,
                    selectedModel = car.model,
                    selectedYear = car.year,
                    selectedFuel = car.fuelType,
                    lastServiceMonth = car.lastServiceDate?.format(DateTimeFormatter.ofPattern("yyyy-MM")) ?: "",
                    insuranceExpiry = car.insuranceExpiry?.format(DateTimeFormatter.ofPattern("yyyy-MM")) ?: "",
                    registrationExpiry = car.registrationExpiry?.format(DateTimeFormatter.ofPattern("yyyy-MM")) ?: "",
                    odometerKm = car.odometerKm?.toString() ?: "",
                    documentPaths = car.attachedDocumentPaths
                ) }
            }
        }
    }

    fun setCountry(code: String, display: String) = _uiState.update { it.copy(countryCode = code, countryDisplay = display) }
    fun selectYear(y: Int) = _uiState.update { it.copy(selectedYear = y) }
    fun selectFuel(f: FuelType) = _uiState.update { it.copy(selectedFuel = f) }
    fun setLastService(v: String) = _uiState.update { it.copy(lastServiceMonth = v) }
    fun setInsuranceExpiry(v: String) = _uiState.update { it.copy(insuranceExpiry = v) }
    fun setRegistrationExpiry(v: String) = _uiState.update { it.copy(registrationExpiry = v) }
    fun setOdometer(v: String) = _uiState.update { it.copy(odometerKm = v) }
    fun addDocuments(paths: List<String>) = _uiState.update { it.copy(documentPaths = it.documentPaths + paths) }
    fun removeDocument(path: String) = _uiState.update { it.copy(documentPaths = it.documentPaths - path) }

    fun onMakeQueryChanged(q: String) {
        _uiState.update { it.copy(
            makeQuery = q,
            filteredMakes = CAR_MAKES.filter { m -> m.contains(q, ignoreCase = true) }.take(8)
        ) }
    }

    fun selectMake(make: String) {
        _uiState.update { it.copy(
            selectedMake = make,
            makeQuery = make,
            filteredMakes = emptyList(),
            availableModels = CAR_MODELS[make] ?: DEFAULT_MODELS,
            selectedModel = ""
        ) }
    }

    fun selectModel(model: String) = _uiState.update { it.copy(selectedModel = model) }

    fun saveCar(onDone: () -> Unit) {
        val s = _uiState.value
        if (s.selectedMake.isBlank() || s.selectedYear == null) return

        viewModelScope.launch {
            _isLoading.value = true
            _uiState.update { it.copy(error = null) }

            val parseFmt = DateTimeFormatter.ofPattern("yyyy-MM")
            val car = Car(
                nickname = "${s.selectedMake} ${s.selectedModel}".trim(),
                make = s.selectedMake,
                model = s.selectedModel.ifBlank { s.selectedMake },
                year = s.selectedYear,
                fuelType = s.selectedFuel,
                countryCode = s.countryCode,
                lastServiceDate = s.lastServiceMonth.takeIf { it.isNotBlank() }
                    ?.let { runCatching { LocalDate.parse("$it-01") }.getOrNull() },
                insuranceExpiry = s.insuranceExpiry.takeIf { it.isNotBlank() }
                    ?.let { runCatching { LocalDate.parse("$it-01") }.getOrNull() },
                registrationExpiry = s.registrationExpiry.takeIf { it.isNotBlank() }
                    ?.let { runCatching { LocalDate.parse("$it-01") }.getOrNull() },
                odometerKm = s.odometerKm.toIntOrNull(),
                attachedDocumentPaths = s.documentPaths
            )

            val carId = carRepository.insertCar(car)
            val savedCar = car.copy(id = carId)
            preferencesManager.setActiveCarId(carId)

            // Attempt AI task generation
            val apiConfig = preferencesManager.apiKeyConfig.firstOrNull()
            if (apiConfig != null) {
                val result = taskRepository.generateAndSaveTasks(savedCar, apiConfig)
                result.onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            } else {
                // No API key — save demo tasks
                taskRepository.insertTasks(demoTasks(carId))
            }

            preferencesManager.setOnboardingDone(true)
            _isLoading.value = false
            onDone()
        }
    }

    private fun demoTasks(carId: Long) = listOf(
        ComplianceTask(carId = carId, title = "Technical inspection",
            category = TaskCategory.LEGAL, dueDate = LocalDate.now().plusMonths(2),
            dueDateWindow = "In ~2 months", status = TaskStatus.UPCOMING,
            urgency = UrgencyLevel.MEDIUM,
            why = "Annual technical inspection is required by law for vehicles over 4 years old in most countries."),
        ComplianceTask(carId = carId, title = "Liability insurance renewal",
            category = TaskCategory.INSURANCE, dueDate = LocalDate.now().plusMonths(3),
            dueDateWindow = "In ~3 months", status = TaskStatus.UPCOMING,
            urgency = UrgencyLevel.MEDIUM,
            why = "Third-party liability insurance is mandatory in most countries. Driving without it can result in significant fines."),
        ComplianceTask(carId = carId, title = "Engine oil & filter change",
            category = TaskCategory.MAINTENANCE, dueDate = LocalDate.now().plusDays(14),
            dueDateWindow = "In ~2 weeks", status = TaskStatus.UPCOMING,
            urgency = UrgencyLevel.HIGH,
            why = "Typical petrol engine oil change interval: 10,000–15,000 km or 12 months. Overdue oil degrades engine components.")
    )

    companion object {
        val COUNTRIES = listOf(
            // ── Europe ────────────────────────────────────────────────────────
            "AL" to "Albania 🇦🇱",
            "AD" to "Andorra 🇦🇩",
            "AT" to "Austria 🇦🇹",
            "BY" to "Belarus 🇧🇾",
            "BE" to "Belgium 🇧🇪",
            "BA" to "Bosnia & Herzegovina 🇧🇦",
            "BG" to "Bulgaria 🇧🇬",
            "HR" to "Croatia 🇭🇷",
            "CY" to "Cyprus 🇨🇾",
            "CZ" to "Czech Republic 🇨🇿",
            "DK" to "Denmark 🇩🇰",
            "EE" to "Estonia 🇪🇪",
            "FI" to "Finland 🇫🇮",
            "FR" to "France 🇫🇷",
            "DE" to "Germany 🇩🇪",
            "GR" to "Greece 🇬🇷",
            "HU" to "Hungary 🇭🇺",
            "IS" to "Iceland 🇮🇸",
            "IE" to "Ireland 🇮🇪",
            "IT" to "Italy 🇮🇹",
            "XK" to "Kosovo 🇽🇰",
            "LV" to "Latvia 🇱🇻",
            "LI" to "Liechtenstein 🇱🇮",
            "LT" to "Lithuania 🇱🇹",
            "LU" to "Luxembourg 🇱🇺",
            "MT" to "Malta 🇲🇹",
            "MD" to "Moldova 🇲🇩",
            "MC" to "Monaco 🇲🇨",
            "ME" to "Montenegro 🇲🇪",
            "NL" to "Netherlands 🇳🇱",
            "MK" to "North Macedonia 🇲🇰",
            "NO" to "Norway 🇳🇴",
            "PL" to "Poland 🇵🇱",
            "PT" to "Portugal 🇵🇹",
            "RO" to "Romania 🇷🇴",
            "RU" to "Russia 🇷🇺",
            "SM" to "San Marino 🇸🇲",
            "RS" to "Serbia 🇷🇸",
            "SK" to "Slovakia 🇸🇰",
            "SI" to "Slovenia 🇸🇮",
            "ES" to "Spain 🇪🇸",
            "SE" to "Sweden 🇸🇪",
            "CH" to "Switzerland 🇨🇭",
            "TR" to "Turkey 🇹🇷",
            "UA" to "Ukraine 🇺🇦",
            "GB" to "United Kingdom 🇬🇧",
            "VA" to "Vatican City 🇻🇦",

            // ── Americas ──────────────────────────────────────────────────────
            "AG" to "Antigua & Barbuda 🇦🇬",
            "AR" to "Argentina 🇦🇷",
            "BS" to "Bahamas 🇧🇸",
            "BB" to "Barbados 🇧🇧",
            "BZ" to "Belize 🇧🇿",
            "BO" to "Bolivia 🇧🇴",
            "BR" to "Brazil 🇧🇷",
            "CA" to "Canada 🇨🇦",
            "CL" to "Chile 🇨🇱",
            "CO" to "Colombia 🇨🇴",
            "CR" to "Costa Rica 🇨🇷",
            "CU" to "Cuba 🇨🇺",
            "DM" to "Dominica 🇩🇲",
            "DO" to "Dominican Republic 🇩🇴",
            "EC" to "Ecuador 🇪🇨",
            "SV" to "El Salvador 🇸🇻",
            "GD" to "Grenada 🇬🇩",
            "GT" to "Guatemala 🇬🇹",
            "GY" to "Guyana 🇬🇾",
            "HT" to "Haiti 🇭🇹",
            "HN" to "Honduras 🇭🇳",
            "JM" to "Jamaica 🇯🇲",
            "MX" to "Mexico 🇲🇽",
            "NI" to "Nicaragua 🇳🇮",
            "PA" to "Panama 🇵🇦",
            "PY" to "Paraguay 🇵🇾",
            "PE" to "Peru 🇵🇪",
            "KN" to "Saint Kitts & Nevis 🇰🇳",
            "LC" to "Saint Lucia 🇱🇨",
            "VC" to "Saint Vincent & Grenadines 🇻🇨",
            "SR" to "Suriname 🇸🇷",
            "TT" to "Trinidad & Tobago 🇹🇹",
            "US" to "United States 🇺🇸",
            "UY" to "Uruguay 🇺🇾",
            "VE" to "Venezuela 🇻🇪",

            // ── Asia ──────────────────────────────────────────────────────────
            "AF" to "Afghanistan 🇦🇫",
            "AM" to "Armenia 🇦🇲",
            "AZ" to "Azerbaijan 🇦🇿",
            "BH" to "Bahrain 🇧🇭",
            "BD" to "Bangladesh 🇧🇩",
            "BT" to "Bhutan 🇧🇹",
            "BN" to "Brunei 🇧🇳",
            "KH" to "Cambodia 🇰🇭",
            "CN" to "China 🇨🇳",
            "GE" to "Georgia 🇬🇪",
            "IN" to "India 🇮🇳",
            "ID" to "Indonesia 🇮🇩",
            "IR" to "Iran 🇮🇷",
            "IQ" to "Iraq 🇮🇶",
            "IL" to "Israel 🇮🇱",
            "JP" to "Japan 🇯🇵",
            "JO" to "Jordan 🇯🇴",
            "KZ" to "Kazakhstan 🇰🇿",
            "KW" to "Kuwait 🇰🇼",
            "KG" to "Kyrgyzstan 🇰🇬",
            "LA" to "Laos 🇱🇦",
            "LB" to "Lebanon 🇱🇧",
            "MY" to "Malaysia 🇲🇾",
            "MV" to "Maldives 🇲🇻",
            "MN" to "Mongolia 🇲🇳",
            "MM" to "Myanmar 🇲🇲",
            "NP" to "Nepal 🇳🇵",
            "KP" to "North Korea 🇰🇵",
            "OM" to "Oman 🇴🇲",
            "PK" to "Pakistan 🇵🇰",
            "PS" to "Palestine 🇵🇸",
            "PH" to "Philippines 🇵🇭",
            "QA" to "Qatar 🇶🇦",
            "SA" to "Saudi Arabia 🇸🇦",
            "SG" to "Singapore 🇸🇬",
            "KR" to "South Korea 🇰🇷",
            "LK" to "Sri Lanka 🇱🇰",
            "SY" to "Syria 🇸🇾",
            "TW" to "Taiwan 🇹🇼",
            "TJ" to "Tajikistan 🇹🇯",
            "TH" to "Thailand 🇹🇭",
            "TL" to "Timor-Leste 🇹🇱",
            "TM" to "Turkmenistan 🇹🇲",
            "AE" to "United Arab Emirates 🇦🇪",
            "UZ" to "Uzbekistan 🇺🇿",
            "VN" to "Vietnam 🇻🇳",
            "YE" to "Yemen 🇾🇪",

            // ── Africa ────────────────────────────────────────────────────────
            "DZ" to "Algeria 🇩🇿",
            "AO" to "Angola 🇦🇴",
            "BJ" to "Benin 🇧🇯",
            "BW" to "Botswana 🇧🇼",
            "BF" to "Burkina Faso 🇧🇫",
            "BI" to "Burundi 🇧🇮",
            "CV" to "Cape Verde 🇨🇻",
            "CM" to "Cameroon 🇨🇲",
            "CF" to "Central African Republic 🇨🇫",
            "TD" to "Chad 🇹🇩",
            "KM" to "Comoros 🇰🇲",
            "CG" to "Congo 🇨🇬",
            "CD" to "DR Congo 🇨🇩",
            "DJ" to "Djibouti 🇩🇯",
            "EG" to "Egypt 🇪🇬",
            "GQ" to "Equatorial Guinea 🇬🇶",
            "ER" to "Eritrea 🇪🇷",
            "SZ" to "Eswatini 🇸🇿",
            "ET" to "Ethiopia 🇪🇹",
            "GA" to "Gabon 🇬🇦",
            "GM" to "Gambia 🇬🇲",
            "GH" to "Ghana 🇬🇭",
            "GN" to "Guinea 🇬🇳",
            "GW" to "Guinea-Bissau 🇬🇼",
            "CI" to "Ivory Coast 🇨🇮",
            "KE" to "Kenya 🇰🇪",
            "LS" to "Lesotho 🇱🇸",
            "LR" to "Liberia 🇱🇷",
            "LY" to "Libya 🇱🇾",
            "MG" to "Madagascar 🇲🇬",
            "MW" to "Malawi 🇲🇼",
            "ML" to "Mali 🇲🇱",
            "MR" to "Mauritania 🇲🇷",
            "MU" to "Mauritius 🇲🇺",
            "MA" to "Morocco 🇲🇦",
            "MZ" to "Mozambique 🇲🇿",
            "NA" to "Namibia 🇳🇦",
            "NE" to "Niger 🇳🇪",
            "NG" to "Nigeria 🇳🇬",
            "RW" to "Rwanda 🇷🇼",
            "ST" to "São Tomé & Príncipe 🇸🇹",
            "SN" to "Senegal 🇸🇳",
            "SC" to "Seychelles 🇸🇨",
            "SL" to "Sierra Leone 🇸🇱",
            "SO" to "Somalia 🇸🇴",
            "ZA" to "South Africa 🇿🇦",
            "SS" to "South Sudan 🇸🇸",
            "SD" to "Sudan 🇸🇩",
            "TZ" to "Tanzania 🇹🇿",
            "TG" to "Togo 🇹🇬",
            "TN" to "Tunisia 🇹🇳",
            "UG" to "Uganda 🇺🇬",
            "ZM" to "Zambia 🇿🇲",
            "ZW" to "Zimbabwe 🇿🇼",

            // ── Oceania ───────────────────────────────────────────────────────
            "AU" to "Australia 🇦🇺",
            "FJ" to "Fiji 🇫🇯",
            "KI" to "Kiribati 🇰🇮",
            "MH" to "Marshall Islands 🇲🇭",
            "FM" to "Micronesia 🇫🇲",
            "NR" to "Nauru 🇳🇷",
            "NZ" to "New Zealand 🇳🇿",
            "PW" to "Palau 🇵🇼",
            "PG" to "Papua New Guinea 🇵🇬",
            "WS" to "Samoa 🇼🇸",
            "SB" to "Solomon Islands 🇸🇧",
            "TO" to "Tonga 🇹🇴",
            "TV" to "Tuvalu 🇹🇻",
            "VU" to "Vanuatu 🇻🇺",
        )
    }
}

val CAR_MAKES = listOf(
    // European
    "Alfa Romeo", "Aston Martin", "Audi", "Bentley", "BMW", "Bugatti",
    "Citroën", "Cupra", "Dacia", "DS Automobiles", "Ferrari", "Fiat",
    "Ford", "Jaguar", "Lamborghini", "Lancia", "Land Rover", "Lotus",
    "Maserati", "McLaren", "Mercedes-Benz", "MINI", "Opel", "Peugeot",
    "Porsche", "Renault", "Rolls-Royce", "Seat", "Škoda", "Smart",
    "Vauxhall", "Volkswagen", "Volvo",
    // Asian
    "BYD", "Chery", "Daihatsu", "Dongfeng", "Geely", "Great Wall",
    "Haval", "Honda", "Hyundai", "Infiniti", "Isuzu", "Kia",
    "Lexus", "Mahindra", "Mazda", "MG", "Mitsubishi", "Nissan",
    "Proton", "Subaru", "Suzuki", "Tata", "Toyota",
    // American
    "Buick", "Cadillac", "Chevrolet", "Chrysler", "Dodge", "Ford",
    "GMC", "Jeep", "Lincoln", "Lucid", "Ram", "Rivian", "Tesla",
    // Other
    "Lada", "Škoda", "SsangYong", "Zastava", "Yugo"
).distinct().sorted()

val CAR_MODELS: Map<String, List<String>> = mapOf(

    // ── European ──────────────────────────────────────────────────────────────

    "Alfa Romeo" to listOf(
        "Giulia", "Giulietta", "Stelvio", "Tonale", "4C", "Brera",
        "147", "156", "159", "166", "MiTo", "Spider"
    ),
    "Aston Martin" to listOf(
        "DB11", "DB12", "DBS", "DBX", "Vantage", "Rapide", "Vanquish", "Virage"
    ),
    "Audi" to listOf(
        "A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8",
        "Q2", "Q3", "Q4 e-tron", "Q5", "Q7", "Q8", "Q8 e-tron",
        "TT", "TTS", "R8", "e-tron GT", "S3", "S4", "S5", "S6", "RS3", "RS6"
    ),
    "Bentley" to listOf(
        "Bentayga", "Continental GT", "Flying Spur", "Mulsanne", "Bacalar"
    ),
    "BMW" to listOf(
        "1 Series", "2 Series", "3 Series", "4 Series", "5 Series",
        "6 Series", "7 Series", "8 Series",
        "X1", "X2", "X3", "X4", "X5", "X6", "X7",
        "iX", "iX1", "iX3", "i3", "i4", "i5", "i7",
        "M2", "M3", "M4", "M5", "M8", "Z4"
    ),
    "Bugatti" to listOf(
        "Chiron", "Veyron", "Bolide", "Divo", "Mistral", "Tourbillon"
    ),
    "Citroën" to listOf(
        "C1", "C2", "C3", "C3 Aircross", "C4", "C4 Cactus", "C5",
        "C5 Aircross", "C5 X", "Berlingo", "Jumpy", "SpaceTourer",
        "ë-C3", "ë-Berlingo", "DS3", "DS4", "DS5"
    ),
    "Cupra" to listOf(
        "Ateca", "Born", "Formentor", "Leon", "Terramar", "Tavascan"
    ),
    "Dacia" to listOf(
        "Duster", "Jogger", "Logan", "Sandero", "Spring",
        "Bigster", "Dokker", "Lodgy", "Solenza"
    ),
    "DS Automobiles" to listOf(
        "DS 3", "DS 3 Crossback", "DS 4", "DS 5", "DS 7", "DS 9"
    ),
    "Ferrari" to listOf(
        "296 GTB", "812 Superfast", "F8 Tributo", "Portofino",
        "Roma", "SF90", "Purosangue", "488", "458", "California"
    ),
    "Fiat" to listOf(
        "500", "500X", "500L", "500e",
        "Tipo", "Panda", "Doblo", "Bravo",
        "Grande Punto", "Linea", "Multipla", "Seicento", "Stilo"
    ),
    "Ford" to listOf(
        "Fiesta", "Focus", "Fusion", "Mondeo", "Mustang", "Mustang Mach-E",
        "Puma", "Kuga", "EcoSport", "Edge", "Explorer", "Bronco",
        "Ranger", "F-150", "Transit", "Galaxy", "S-Max",
        "C-Max", "B-Max", "Ka", "Ka+"
    ),
    "Jaguar" to listOf(
        "E-Pace", "F-Pace", "F-Type", "I-Pace",
        "XE", "XF", "XJ", "XK", "S-Type", "X-Type"
    ),
    "Lamborghini" to listOf(
        "Aventador", "Huracán", "Urus", "Revuelto", "Sterrato", "Countach"
    ),
    "Lancia" to listOf(
        "Delta", "Ypsilon", "Thema", "Stratos", "Musa", "Phedra"
    ),
    "Land Rover" to listOf(
        "Defender", "Discovery", "Discovery Sport",
        "Freelander", "Range Rover", "Range Rover Evoque",
        "Range Rover Sport", "Range Rover Velar"
    ),
    "Lotus" to listOf(
        "Eletre", "Emira", "Elise", "Exige", "Evora", "Esprit"
    ),
    "Maserati" to listOf(
        "Ghibli", "GranTurismo", "GranCabrio", "Grecale",
        "Levante", "Quattroporte", "MC20"
    ),
    "McLaren" to listOf(
        "Artura", "GT", "720S", "750S", "570S", "600LT", "Elva", "Senna"
    ),
    "Mercedes-Benz" to listOf(
        "A-Class", "B-Class", "C-Class", "E-Class", "S-Class",
        "CLA", "CLS", "GLA", "GLB", "GLC", "GLE", "GLS",
        "EQA", "EQB", "EQC", "EQE", "EQS",
        "AMG GT", "G-Class", "Maybach S", "SL", "SLC",
        "V-Class", "Vito", "Sprinter"
    ),
    "MINI" to listOf(
        "Cooper", "Cooper S", "Clubman", "Countryman",
        "Convertible", "Paceman", "Coupe", "Roadster", "Aceman"
    ),
    "Opel" to listOf(
        "Astra", "Corsa", "Insignia", "Mokka", "Mokka-e",
        "Crossland", "Grandland", "Zafira", "Zafira-e",
        "Vivaro", "Adam", "Agila", "Meriva", "Vectra", "Omega"
    ),
    "Peugeot" to listOf(
        "107", "108", "206", "207", "208", "e-208",
        "2008", "e-2008", "306", "307", "308",
        "3008", "408", "4008", "5008", "508", "508 SW",
        "Partner", "Rifter", "Traveller"
    ),
    "Porsche" to listOf(
        "911", "718 Boxster", "718 Cayman", "Cayenne",
        "Macan", "Panamera", "Taycan", "Taycan Cross Turismo"
    ),
    "Renault" to listOf(
        "Clio", "Megane", "Megane E-Tech", "Laguna", "Talisman",
        "Captur", "Kadjar", "Austral", "Arkana",
        "Duster", "Koleos", "Scenic",
        "Zoe", "Twingo", "Symbol", "Fluence",
        "Espace", "Kangoo", "Trafic"
    ),
    "Rolls-Royce" to listOf(
        "Ghost", "Phantom", "Silver Shadow", "Spectre",
        "Wraith", "Dawn", "Cullinan"
    ),
    "Seat" to listOf(
        "Ibiza", "Leon", "Arona", "Ateca", "Tarraco",
        "Alhambra", "Altea", "Exeo", "Toledo", "Mii"
    ),
    "Škoda" to listOf(
        "Fabia", "Scala", "Octavia", "Superb",
        "Kamiq", "Karoq", "Kodiaq", "Enyaq",
        "Felicia", "Rapid", "Roomster", "Yeti"
    ),
    "Smart" to listOf(
        "Fortwo", "Forfour", "Formore", "#1", "#3"
    ),
    "Vauxhall" to listOf(
        "Astra", "Corsa", "Insignia", "Mokka",
        "Crossland", "Grandland", "Zafira", "Adam", "Meriva"
    ),
    "Volkswagen" to listOf(
        "Polo", "Golf", "Golf Plus", "Golf Variant", "Golf Alltrack",
        "Passat", "Passat Variant", "Arteon",
        "Tiguan", "Tiguan Allspace", "Touareg", "T-Cross", "T-Roc",
        "ID.3", "ID.4", "ID.5", "ID.6", "ID.7",
        "Caddy", "Touran", "Sharan", "Multivan",
        "Up", "Beetle", "Phaeton", "Eos"
    ),
    "Volvo" to listOf(
        "C30", "C40", "S40", "S60", "S80", "S90",
        "V40", "V50", "V60", "V70", "V90",
        "XC40", "XC60", "XC70", "XC90",
        "EX30", "EX40", "EX90", "EC40"
    ),

    // ── Asian ─────────────────────────────────────────────────────────────────

    "BYD" to listOf(
        "Atto 3", "Han", "Tang", "Song", "Qin",
        "Seal", "Dolphin", "Seagull", "Sealion 6"
    ),
    "Chery" to listOf(
        "Tiggo 4", "Tiggo 5X", "Tiggo 7", "Tiggo 8",
        "Arrizo 5", "Arrizo 6", "QQ"
    ),
    "Daihatsu" to listOf(
        "Terios", "Sirion", "Materia", "Copen", "Move", "Rocky", "Charade"
    ),
    "Geely" to listOf(
        "Emgrand", "Coolray", "Atlas", "Tugella", "Okavango"
    ),
    "Great Wall" to listOf(
        "Haval H6", "Haval H9", "Poer", "Wingle", "Jolion"
    ),
    "Haval" to listOf(
        "H1", "H2", "H4", "H6", "H9", "Jolion", "Dargo"
    ),
    "Honda" to listOf(
        "Civic", "Accord", "Jazz", "CR-V", "HR-V", "Pilot",
        "Fit", "City", "Stream", "FR-V", "Legend",
        "CR-Z", "Insight", "e", "ZR-V", "Passport"
    ),
    "Hyundai" to listOf(
        "i10", "i20", "i30", "i40",
        "Elantra", "Sonata", "Tucson", "Santa Fe",
        "Kona", "Nexo", "Veloster",
        "IONIQ", "IONIQ 5", "IONIQ 6",
        "Staria", "Terracan", "ix35", "ix55"
    ),
    "Infiniti" to listOf(
        "Q30", "Q50", "Q60", "Q70",
        "QX30", "QX50", "QX55", "QX60", "QX70", "QX80"
    ),
    "Isuzu" to listOf(
        "D-Max", "MU-X", "Rodeo", "Trooper", "Gemini"
    ),
    "Kia" to listOf(
        "Picanto", "Rio", "Ceed", "ProCeed", "Xceed",
        "Stonic", "Sportage", "Sorento", "Telluride",
        "Stinger", "Niro", "EV6", "EV9",
        "Carnival", "Soul", "Optima", "Cerato"
    ),
    "Lexus" to listOf(
        "CT", "ES", "GS", "IS", "LS", "LC", "LM",
        "NX", "RX", "GX", "LX", "UX", "RZ"
    ),
    "Mahindra" to listOf(
        "Thar", "Scorpio", "XUV700", "XUV300", "Bolero", "KUV100"
    ),
    "Mazda" to listOf(
        "Mazda2", "Mazda3", "Mazda6",
        "CX-3", "CX-30", "CX-5", "CX-60", "CX-80", "CX-90",
        "MX-5", "MX-30", "RX-8", "BT-50"
    ),
    "MG" to listOf(
        "3", "5", "6", "ZS", "ZS EV",
        "HS", "MG4", "Cyberster", "Marvel R"
    ),
    "Mitsubishi" to listOf(
        "Colt", "Lancer", "Eclipse Cross", "Galant",
        "Outlander", "ASX", "Pajero", "Pajero Sport",
        "L200", "Space Star", "i-MiEV"
    ),
    "Nissan" to listOf(
        "Micra", "Note", "Juke", "Qashqai",
        "X-Trail", "Murano", "Pathfinder", "Patrol",
        "Leaf", "Ariya", "370Z", "GT-R",
        "Navara", "Terrano", "Almera", "Primera"
    ),
    "Proton" to listOf(
        "Saga", "Persona", "Iriz", "Exora", "X50", "X70"
    ),
    "Subaru" to listOf(
        "Impreza", "Legacy", "Outback", "Forester",
        "XV", "Crosstrek", "BRZ", "WRX",
        "Solterra", "Ascent", "Levorg"
    ),
    "Suzuki" to listOf(
        "Alto", "Swift", "Baleno", "Ignis",
        "Vitara", "S-Cross", "Jimny",
        "SX4", "Celerio", "Ertiga", "Grand Vitara"
    ),
    "Tata" to listOf(
        "Tiago", "Tigor", "Altroz", "Nexon",
        "Harrier", "Safari", "Punch", "Curvv"
    ),
    "Toyota" to listOf(
        "Aygo", "Aygo X", "Yaris", "Yaris Cross",
        "Corolla", "Camry", "Avensis",
        "C-HR", "RAV4", "Highlander",
        "Land Cruiser", "Land Cruiser Prado",
        "Hilux", "Prius", "bZ4X",
        "GR86", "GR Yaris", "Supra",
        "Proace", "Verso", "Auris"
    ),

    // ── American ──────────────────────────────────────────────────────────────

    "Buick" to listOf(
        "Encore", "Encore GX", "Envision", "Enclave",
        "LaCrosse", "Regal", "Verano"
    ),
    "Cadillac" to listOf(
        "CT4", "CT5", "Escalade", "XT4", "XT5", "XT6",
        "Lyriq", "Celestiq", "SRX", "ATS", "CTS"
    ),
    "Chevrolet" to listOf(
        "Spark", "Sonic", "Cruze", "Malibu", "Impala",
        "Camaro", "Corvette",
        "Trax", "Equinox", "Blazer", "Traverse",
        "Tahoe", "Suburban",
        "Colorado", "Silverado",
        "Bolt EV", "Bolt EUV"
    ),
    "Chrysler" to listOf(
        "300", "Pacifica", "Voyager", "PT Cruiser", "Sebring"
    ),
    "Dodge" to listOf(
        "Charger", "Challenger", "Durango", "Journey",
        "Dart", "Viper", "Hornet"
    ),
    "GMC" to listOf(
        "Terrain", "Equinox", "Acadia", "Yukon",
        "Sierra", "Canyon", "Hummer EV"
    ),
    "Jeep" to listOf(
        "Renegade", "Compass", "Cherokee",
        "Grand Cherokee", "Wrangler", "Gladiator",
        "Avenger", "Wagoneer"
    ),
    "Lincoln" to listOf(
        "Corsair", "Nautilus", "Aviator",
        "Navigator", "Continental", "MKZ"
    ),
    "Lucid" to listOf(
        "Air", "Gravity"
    ),
    "Ram" to listOf(
        "1500", "2500", "3500", "ProMaster", "ProMaster City"
    ),
    "Rivian" to listOf(
        "R1T", "R1S", "R2", "R3"
    ),
    "Tesla" to listOf(
        "Model 3", "Model Y", "Model S", "Model X",
        "Cybertruck", "Roadster", "Semi"
    ),

    // ── Other / Regional ──────────────────────────────────────────────────────

    "Lada" to listOf(
        "Vesta", "Granta", "Niva", "Niva Legend", "Niva Travel",
        "2101", "2105", "2107", "2110", "Samara", "Kalina", "Priora"
    ),
    "SsangYong" to listOf(
        "Tivoli", "Korando", "Rexton", "Musso", "Rodius", "Actyon"
    ),
    "Zastava" to listOf(
        "10", "101", "128", "750", "Skala", "Florida"
    ),
    "Yugo" to listOf(
        "45", "55", "60", "65", "GV", "Sana"
    ),
)

val DEFAULT_MODELS = listOf(
    "Standard", "Comfort", "Sport", "LX", "EX", "SE",
    "Premium", "Base", "Limited", "Touring", "Elite", "Executive"
)

// ── MainViewModel ─────────────────────────────────────────────────────────────

@HiltViewModel
class MainViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val taskRepository: TaskRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val cars: StateFlow<List<Car>> = carRepository.getAllCars()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeCarId = MutableStateFlow<Long?>(null)

    val activeCar: StateFlow<Car?> = combine(cars, _activeCarId) { carList, activeId ->
        if (activeId != null) carList.firstOrNull { it.id == activeId }
        else carList.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tasks: StateFlow<List<ComplianceTask>> = activeCar.flatMapLatest { car ->
        if (car != null) taskRepository.getActiveTasksForCar(car.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val apiError: StateFlow<String?> = preferencesManager.lastApiError
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.activeCarId.firstOrNull()?.let { _activeCarId.value = it }
        }
    }

    fun selectCar(car: Car) {
        _activeCarId.value = car.id
        viewModelScope.launch { preferencesManager.setActiveCarId(car.id) }
    }

    fun refreshTasks(car: Car) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val apiConfig = preferencesManager.apiKeyConfig.firstOrNull()
            if (apiConfig != null) {
                taskRepository.generateAndSaveTasks(car, apiConfig)
            }
            _isRefreshing.value = false
        }
    }

    fun markDone(taskId: Long) = viewModelScope.launch { taskRepository.markDone(taskId) }

    fun snoozeTask(taskId: Long) = viewModelScope.launch { taskRepository.snoozeTask(taskId) }

    fun editTask(task: ComplianceTask) {
        // Edit is handled via dialog in the UI layer; update via updateTask
        viewModelScope.launch { taskRepository.updateTask(task) }
    }

    fun deleteCar(car: Car) {
        viewModelScope.launch {
            carRepository.deleteCar(car)
            if (_activeCarId.value == car.id) _activeCarId.value = null
        }
    }
}

// ── SettingsViewModel ─────────────────────────────────────────────────────────

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val notifPrefs: StateFlow<NotificationPreferences?> = preferencesManager.notificationPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val apiKeyConfig: StateFlow<ApiKeyConfig?> = preferencesManager.apiKeyConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateNotifPrefs(prefs: NotificationPreferences) {
        viewModelScope.launch { preferencesManager.updateNotificationPreferences(prefs) }
    }
}
