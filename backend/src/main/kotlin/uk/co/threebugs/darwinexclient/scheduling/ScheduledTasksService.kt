package uk.co.threebugs.darwinexclient.scheduling

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.co.threebugs.darwinexclient.setupgroup.S3SetupGroupService

/**
 * Service for handling scheduled tasks in the application
 */
@Service
class ScheduledTasksService(
    private val s3SetupGroupService: S3SetupGroupService
) {
    private val logger = LoggerFactory.getLogger(ScheduledTasksService::class.java)
    
    /**
     * Scheduled task to update SetupGroups from S3 every 10 minutes
     * Runs at minutes 0, 10, 20, 30, 40, 50 of every hour
     */
    @Scheduled(cron = "0 */10 * * * *")
    fun updateSetupGroupsFromS3() {
        logger.info("Running scheduled update of SetupGroups from S3")
        try {
            val createdCount = s3SetupGroupService.updateSetupGroupsFromS3()
            logger.info("Scheduled update completed. Created $createdCount new SetupGroups")
        } catch (e: Exception) {
            logger.error("Error during scheduled update of SetupGroups from S3", e)
        }
    }
}