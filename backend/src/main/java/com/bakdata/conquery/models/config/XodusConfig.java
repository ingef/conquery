package com.bakdata.conquery.models.config;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.google.common.primitives.Ints;
import io.dropwizard.util.Duration;
import io.dropwizard.util.Size;
import io.dropwizard.util.SizeUnit;
import io.dropwizard.validation.MaxSize;
import jetbrains.exodus.env.EnvironmentConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class XodusConfig {
	private Size	memoryUsage;
	private Integer	memoryUsagePercentage;
	private String	cipherId;
	private String	cipherKey;
	private Long	cipherBasicIV;
	private Boolean	logDurableWrite;
	@MaxSize(value=1,unit=SizeUnit.GIGABYTES)
	private Size	logFileSize = Size.megabytes(400L);
	private String	logLockId;
	private Duration logLockTimeout = Duration.seconds(1);
	private Size	logCachePageSize;
	private Integer	logCacheOpenFilesCount;
	private Boolean	logCacheUseNio;
	private Long	logCacheFreePhysicalMemoryThreshold;
	private Boolean	logCacheShared;
	private Boolean	logCacheNonBlocking;
	private Boolean	logCleanDirectoryExpected;
	private Boolean	logClearInvalid;
	private Long	logSyncPeriod;
	private Boolean	fullFileReadonly;
	private Boolean	envIsReadonly;
	private Boolean	envReadonlyEmptyStores;
	private Integer	envStoreGetCacheSize;
	private Boolean	envCloseForcedly;
	private Long	envTxnReplayTimeout;
	private Integer	envTxnReplayMaxCount;
	private Boolean	envTxnDowngradeAfterFlush;
	private Integer	envMaxParallelTxns;
	private Integer	envMaxParallelReadonlyTxns;
	@NotNull @Valid
	private Duration envMonitorTxnsTimeout = Duration.minutes(10);
	private Integer	envMonitorTxnsCheckFreq;
	private Boolean	envGatherStatistics;
	private Integer	treeMaxPageSize;
	private Boolean	gcEnabled;
	private Integer	gcStartIn;
	private Integer	gcMinUtilization;
	private Boolean	gcRenameFiles;
	private Integer	gcFileMinAge;
	private Integer	gcFilesInterval;
	private Integer	gcRunPeriod;
	private Boolean	gcUtilizationFromScratch;
	private String	gcUtilizationFromFile;
	private Boolean	gcUseExclusiveTransaction;
	private Integer	gcTransactionAcquireTimeout;
	private Integer	gcTransactionTimeout;
	private Integer	gcFilesDeletionDelay;
	private Boolean	managementEnabled;
	private Boolean	managementOperationsRestricted;
	
	public EnvironmentConfig createConfig() {
		EnvironmentConfig config = new EnvironmentConfig();
		if(memoryUsage != null) {config.setMemoryUsage(memoryUsage.toBytes());}
		if(memoryUsagePercentage != null) {config.setMemoryUsagePercentage(memoryUsagePercentage);}
		if(cipherId != null) {config.setCipherId(cipherId);}
		if(cipherKey != null) {config.setCipherKey(cipherKey);}
		if(cipherBasicIV != null) {config.setCipherBasicIV(cipherBasicIV);}
		if(logDurableWrite != null) {config.setLogDurableWrite(logDurableWrite);}
		if(logFileSize != null) {config.setLogFileSize(logFileSize.toKilobytes());}
		if(logLockId != null) {config.setLogLockId(logLockId);}
		if(logLockTimeout != null) {config.setLogLockTimeout(logLockTimeout.toMilliseconds());}
		if(logCachePageSize != null) {config.setLogCachePageSize(Ints.checkedCast(logCachePageSize.toBytes()));}
		if(logCacheOpenFilesCount != null) {config.setLogCacheOpenFilesCount(logCacheOpenFilesCount);}
		if(logCacheUseNio != null) {config.setLogCacheUseNio(logCacheUseNio);}
		if(logCacheFreePhysicalMemoryThreshold != null) {config.setLogCacheFreePhysicalMemoryThreshold(logCacheFreePhysicalMemoryThreshold);}
		if(logCacheShared != null) {config.setLogCacheShared(logCacheShared);}
		if(logCacheNonBlocking != null) {config.setLogCacheNonBlocking(logCacheNonBlocking);}
		if(logCleanDirectoryExpected != null) {config.setLogCleanDirectoryExpected(logCleanDirectoryExpected);}
		if(logClearInvalid != null) {config.setLogClearInvalid(logClearInvalid);}
		if(logSyncPeriod != null) {config.setLogSyncPeriod(logSyncPeriod);}
		if(fullFileReadonly != null) {config.setFullFileReadonly(fullFileReadonly);}
		if(envIsReadonly != null) {config.setEnvIsReadonly(envIsReadonly);}
		if(envReadonlyEmptyStores != null) {config.setEnvReadonlyEmptyStores(envReadonlyEmptyStores);}
		if(envStoreGetCacheSize != null) {config.setEnvStoreGetCacheSize(envStoreGetCacheSize);}
		if(envCloseForcedly != null) {config.setEnvCloseForcedly(envCloseForcedly);}
		if(envTxnReplayTimeout != null) {config.setEnvTxnReplayTimeout(envTxnReplayTimeout);}
		if(envTxnReplayMaxCount != null) {config.setEnvTxnReplayMaxCount(envTxnReplayMaxCount);}
		if(envTxnDowngradeAfterFlush != null) {config.setEnvTxnDowngradeAfterFlush(envTxnDowngradeAfterFlush);}
		if(envMaxParallelTxns != null) {config.setEnvMaxParallelTxns(envMaxParallelTxns);}
		if(envMaxParallelReadonlyTxns != null) {config.setEnvMaxParallelReadonlyTxns(envMaxParallelReadonlyTxns);}
		if(envMonitorTxnsTimeout != null) {config.setEnvMonitorTxnsTimeout(Ints.checkedCast(envMonitorTxnsTimeout.toMilliseconds()));}
		if(envMonitorTxnsCheckFreq != null) {config.setEnvMonitorTxnsCheckFreq(envMonitorTxnsCheckFreq);}
		if(envGatherStatistics != null) {config.setEnvGatherStatistics(envGatherStatistics);}
		if(treeMaxPageSize != null) {config.setTreeMaxPageSize(treeMaxPageSize);}
		if(gcEnabled != null) {config.setGcEnabled(gcEnabled);}
		if(gcStartIn != null) {config.setGcStartIn(gcStartIn);}
		if(gcMinUtilization != null) {config.setGcMinUtilization(gcMinUtilization);}
		if(gcRenameFiles != null) {config.setGcRenameFiles(gcRenameFiles);}
		if(gcFileMinAge != null) {config.setGcFileMinAge(gcFileMinAge);}
		if(gcFilesInterval != null) {config.setGcFilesInterval(gcFilesInterval);}
		if(gcRunPeriod != null) {config.setGcRunPeriod(gcRunPeriod);}
		if(gcUtilizationFromScratch != null) {config.setGcUtilizationFromScratch(gcUtilizationFromScratch);}
		if(gcUtilizationFromFile != null) {config.setGcUtilizationFromFile(gcUtilizationFromFile);}
		if(gcUseExclusiveTransaction != null) {config.setGcUseExclusiveTransaction(gcUseExclusiveTransaction);}
		if(gcTransactionAcquireTimeout != null) {config.setGcTransactionAcquireTimeout(gcTransactionAcquireTimeout);}
		if(gcTransactionTimeout != null) {config.setGcTransactionTimeout(gcTransactionTimeout);}
		if(gcFilesDeletionDelay != null) {config.setGcFilesDeletionDelay(gcFilesDeletionDelay);}
		if(managementEnabled != null) {config.setManagementEnabled(managementEnabled);}
		if(managementOperationsRestricted != null) {config.setManagementOperationsRestricted(managementOperationsRestricted);}
		
		return config;
	}
}
