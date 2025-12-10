package io.sandboxdev.gitmcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configuration properties for Git MCP Server.
 */
@Component
@ConfigurationProperties(prefix = "git-mcp-server")
public class GitMcpServerProperties {
    
    private Repository repository = new Repository();
    private History history = new History();
    private Authentication authentication = new Authentication();
    private Mcp mcp = new Mcp();
    
    public Repository getRepository() {
        return repository;
    }
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public History getHistory() {
        return history;
    }
    
    public void setHistory(History history) {
        this.history = history;
    }
    
    public Authentication getAuthentication() {
        return authentication;
    }
    
    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }
    
    public Mcp getMcp() {
        return mcp;
    }
    
    public void setMcp(Mcp mcp) {
        this.mcp = mcp;
    }
    
    public static class Repository {
        private Cache cache = new Cache();
        
        public Cache getCache() {
            return cache;
        }
        
        public void setCache(Cache cache) {
            this.cache = cache;
        }
        
        public static class Cache {
            private int maxSize = 10;
            private Duration timeout = Duration.ofMinutes(30);
            
            public int getMaxSize() {
                return maxSize;
            }
            
            public void setMaxSize(int maxSize) {
                this.maxSize = maxSize;
            }
            
            public Duration getTimeout() {
                return timeout;
            }
            
            public void setTimeout(Duration timeout) {
                this.timeout = timeout;
            }
        }
    }
    
    public static class History {
        private int maxLimit = 1000;
        
        public int getMaxLimit() {
            return maxLimit;
        }
        
        public void setMaxLimit(int maxLimit) {
            this.maxLimit = maxLimit;
        }
    }
    
    public static class Authentication {
        private boolean enabled = true;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
    
    public static class Mcp {
        private String serverName = "Git MCP Server";
        private String serverVersion = "1.0.0";
        
        public String getServerName() {
            return serverName;
        }
        
        public void setServerName(String serverName) {
            this.serverName = serverName;
        }
        
        public String getServerVersion() {
            return serverVersion;
        }
        
        public void setServerVersion(String serverVersion) {
            this.serverVersion = serverVersion;
        }
    }
}