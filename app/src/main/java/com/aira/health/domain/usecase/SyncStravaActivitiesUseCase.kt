package com.aira.health.domain.usecase

import com.aira.health.domain.model.StravaSyncSummary
import com.aira.health.domain.repository.StravaRepository
import javax.inject.Inject

class SyncStravaActivitiesUseCase @Inject constructor(
    private val stravaRepository: StravaRepository
) {
    suspend operator fun invoke(maxPagesPerRun: Int = 6): Result<StravaSyncSummary> {
        return stravaRepository.syncActivities(maxPagesPerRun)
    }
}
