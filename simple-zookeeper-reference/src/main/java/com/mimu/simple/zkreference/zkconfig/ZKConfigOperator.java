package com.mimu.simple.zkreference.zkconfig;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.config.DynamicWatchedConfiguration;
import com.netflix.config.source.ZooKeeperConfigurationSource;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ZKConfigOperator {
    private static final Logger logger = LoggerFactory.getLogger(ZKConfigOperator.class);
    private static final AtomicBoolean initialization = new AtomicBoolean(false);
    private ZKConfigResource zkConfigResource;

    private CuratorFramework client;
    private ZooKeeperConfigurationSource zkConfigSource;

    public ZKConfigOperator(String address, String path) {
        ZKConfigResource zkConfigResource = new ZKConfigResource();
        zkConfigResource.setZkAddress(address);
        zkConfigResource.setZkPath(path);
        this.zkConfigResource = zkConfigResource;
        if (initialization.compareAndSet(false, true)) {
            initAndStartConfiguration();
        }
    }

    public ZKConfigResource getZkConfigResource() {
        return zkConfigResource;
    }

    private void initAndStartConfiguration() {
        client = CuratorFrameworkFactory.newClient(getZkConfigResource().getZkAddress(), new ExponentialBackoffRetry(1000, 1));
        client.start();
        zkConfigSource = new ZooKeeperConfigurationSource(client, getZkConfigResource().getZkPath());
        try {
            zkConfigSource.start();
        } catch (Exception e) {
            logger.error("initAndStartConfiguration error", e);
        }
        DynamicWatchedConfiguration zkWatchedConfig = new DynamicWatchedConfiguration(zkConfigSource);
        ConfigurationManager.install(zkWatchedConfig);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getCurrentData() {
        try {
            Map<String, Object> currentData = zkConfigSource.getCurrentData();
            Map<String, String> result = new HashMap<>();
            for (Map.Entry<String, Object> next : currentData.entrySet()) {
                result.put(next.getKey(), String.valueOf(next.getValue()));
            }
            return result;
        } catch (Exception e) {
            logger.error("getCurrentData error", e);
            return Collections.EMPTY_MAP;
        }
    }

    public DynamicStringProperty getString(String key, String defaultValue) {
        return getString(key, defaultValue, null);
    }

    public DynamicStringProperty getString(String key, String defaultValue, Runnable callable) {
        return DynamicPropertyFactory.getInstance().getStringProperty(key, defaultValue, callable);
    }
}
