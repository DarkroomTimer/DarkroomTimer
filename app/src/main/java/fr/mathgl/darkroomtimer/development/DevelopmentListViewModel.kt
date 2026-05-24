package fr.mathgl.darkroomtimer.development

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel pour la liste des profils de d&#xe9;veloppement.
 * G&#xe9;re les operations CRUD sur les profils et la s&#xe9;lection.
 */
class DevelopmentListViewModel(
    application: Application,
    private val dao: DevelopmentDao
) : AndroidViewModel(application) {

    private val _profiles = MutableStateFlow<List<DevelopmentProfile>>(emptyList())
    val profiles: StateFlow<List<DevelopmentProfile>> = _profiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedProfile = MutableStateFlow<DevelopmentProfile?>(null)
    val selectedProfile: StateFlow<DevelopmentProfile?> = _selectedProfile.asStateFlow()

    private val _showEditor = MutableStateFlow(false)
    val showEditor: StateFlow<Boolean> = _showEditor.asStateFlow()

    // Note: init block removed to avoid coroutine dispatch issues in tests.
    // Call loadProfiles() explicitly when needed.

    /** Charge tous les profils depuis la base de données */
    fun loadProfiles() {
        _isLoading.value = true
        viewModelScope.launch {
            dao.getAllProfiles().collect { entities ->
                val domainProfiles = entities.map { it.toDomain() }.sortedBy { it.name }
                _profiles.value = domainProfiles
                _isLoading.value = false
            }
        }
    }

    /** S&#xe9;lecte un profil pour l&#xe9;dition ou la session */
    fun selectProfile(profile: DevelopmentProfile) {
        _selectedProfile.value = profile
    }

    /** D&#xe9;osele le profil s&#xe9;lectionn&#xe9; */
    fun deselectProfile() {
        _selectedProfile.value = null
    }

    /** Ouvre l&#xe9;diteur pour un nouveau ou existant profil */
    fun openEditor(profile: DevelopmentProfile?) {
        _selectedProfile.value = profile
        _showEditor.value = true
    }

    /** Ferme l&#xe9;diteur */
    fun closeEditor() {
        _showEditor.value = false
        _selectedProfile.value = null
    }

    /** Sauvegarde un profil (cr&#xe9;ation ou mise &#xe0;jour) */
    fun saveProfile(profile: DevelopmentProfile) {
        viewModelScope.launch {
            if (profile.id == 0L) {
                // Nouvelle cr&#xe9;ation
                val entity = DevelopmentProfileEntity.fromDomain(profile)
                val newId = dao.insertProfile(entity)
                val savedProfile = profile.copy(id = newId)
                updateProfileList { it + savedProfile }
            } else {
                // Mise &#xe0;jour
                val entity = DevelopmentProfileEntity.fromDomain(profile)
                dao.updateProfile(entity)
                updateProfileList { list ->
                    list.map { if (it.id == profile.id) profile else it }
                }
            }
        }
    }

    /** Supprime un profil */
    fun deleteProfile(profile: DevelopmentProfile) {
        viewModelScope.launch {
            dao.deleteProfileById(profile.id)
            // La liste sera mise &#xe0;jour automatiquement via getAllProfiles()
        }
    }

    /** Cr&#xe9;e un profil vide avec le nom donn&#xe9; */
    fun createEmptyProfile(name: String, mode: DevelopmentNavigationMode = DevelopmentNavigationMode.MANUAL): DevelopmentProfile {
        return DevelopmentProfile(
            id = 0L,
            name = name,
            navigationMode = mode,
            steps = emptyList(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun updateProfileList(mutator: (List<DevelopmentProfile>) -> List<DevelopmentProfile>) {
        _profiles.value = mutator(_profiles.value)
    }

    /** Factory pour cr&#xe9;er le ViewModel avec un Application et un DAO */
    companion object Factory {
        fun create(
            application: Application,
            dao: DevelopmentDao
        ): DevelopmentListViewModel {
            return DevelopmentListViewModel(application, dao)
        }
    }
}
