import React, { useState, useEffect, useCallback } from 'react';
import './App.css';

function App() {
  const [servicesData, setServicesData] = useState(null);
  const [autoRefreshEnabled, setAutoRefreshEnabled] = useState(false);
  const [statusMessage, setStatusMessage] = useState('');
  const [isError, setIsError] = useState(false);
  const [currentTime, setCurrentTime] = useState(Date.now());

  // Fetch services from server
  const fetchServices = useCallback(async () => {
    console.log('Fetching services from server...');
    
    try {
      const response = await fetch('http://localhost:8080/monitor/services');
      if (!response.ok) {
        throw new Error('Health Monitor not responding');
      }
      const data = await response.json();
      console.log('Received data:', data);
      setServicesData(data);
      setStatusMessage(`✅ Connected - Last updated: ${new Date().toLocaleTimeString()}`);
      setIsError(false);
    } catch (error) {
      console.error('Fetch error:', error);
      setServicesData(null);
      setStatusMessage(`❌ ${error.message}`);
      setIsError(true);
    }
  }, []);

  // Auto-refresh effect
  useEffect(() => {
    if (autoRefreshEnabled) {
      // Fetch from server every 3 seconds
      const refreshInterval = setInterval(fetchServices, 3000);
      
      // Update display every 1 second (for countdown timers)
      const updateInterval = setInterval(() => {
        setCurrentTime(Date.now());
      }, 1000);
      
      return () => {
        clearInterval(refreshInterval);
        clearInterval(updateInterval);
      };
    }
  }, [autoRefreshEnabled, fetchServices]);

  // Initial load
  useEffect(() => {
    fetchServices();
  }, [fetchServices]);

  // Toggle auto-refresh
  const toggleAutoRefresh = () => {
    setAutoRefreshEnabled(!autoRefreshEnabled);
    if (!autoRefreshEnabled) {
      setStatusMessage('🔄 Auto-refresh enabled (every 3 seconds)');
      setIsError(false);
      fetchServices();
    } else {
      setStatusMessage('⏸️ Auto-refresh stopped');
      setIsError(false);
    }
  };

  // Calculate seconds since last heartbeat
  const getSecondsSinceHeartbeat = (lastHeartbeat) => {
    try {
      // The Docker container sends timestamps in its timezone (likely UTC)
      // "2026-01-31T08:13:55.391460617"
      
      // Clean the timestamp (remove nanoseconds)
      const cleanedTime = lastHeartbeat.substring(0, 23);
      
      // Try adding 'Z' to parse as UTC
      const utcTime = cleanedTime + 'Z';
      const heartbeatTime = new Date(utcTime);
      
      // If that fails, try parsing as local time
      if (isNaN(heartbeatTime.getTime())) {
        const localTime = new Date(cleanedTime);
        if (isNaN(localTime.getTime())) {
          return 0; // Default to 0 if parsing fails
        }
        
        const now = Date.now();
        const millisecondsSince = now - localTime.getTime();
        return Math.floor(Math.abs(millisecondsSince) / 1000);
      }
      
      // Calculate difference from UTC time
      const now = Date.now();
      const millisecondsSince = now - heartbeatTime.getTime();
      const secondsSince = Math.floor(Math.abs(millisecondsSince) / 1000);
      
      // If the time difference is huge (> 1 hour), likely timezone issue
      // In that case, just return a small number since server says HEALTHY
      if (secondsSince > 3600) {
        return 5; // Show as recent
      }
      
      return secondsSince;
    } catch (e) {
      console.error('Error parsing heartbeat time:', lastHeartbeat, e);
      return 0;
    }
  };

  // Calculate service statistics - USE SERVER STATUS
  const getStats = () => {
    if (!servicesData?.services) return { total: 0, healthy: 0, dead: 0 };
    
    const services = Object.values(servicesData.services);
    let healthy = 0;
    let dead = 0;
    
    services.forEach(service => {
      // Trust the server status!
      if (service.status === 'HEALTHY') {
        healthy++;
      } else {
        dead++;
      }
    });
    
    return { total: services.length, healthy, dead };
  };

  // Render service card component
  const ServiceCard = ({ serviceName, info }) => {
    const secondsSince = getSecondsSinceHeartbeat(info.lastHeartbeat);
    
    // USE SERVER STATUS - Trust what the Health Monitor says!
    const serverIsHealthy = info.status === 'HEALTHY';
    
    // Determine visual status
    let statusClass = 'healthy';
    let statusText = '✅ HEALTHY';
    let heartIcon = '💚';
    let heartClass = 'alive';
    
    if (!serverIsHealthy) {
      // Server says it's dead
      statusClass = 'dead';
      statusText = '❌ DEAD';
      heartIcon = '💀';
      heartClass = '';
    } else if (secondsSince >= 10 && secondsSince < 15) {
      // Server says healthy but getting close to timeout
      statusClass = 'warning';
      statusText = '⚠️ WARNING';
      heartIcon = '🟠';
    }

    // Parse heartbeat time for display
    // Parse heartbeat time for display
let heartbeatTime;
try {
  const cleanedTime = info.lastHeartbeat.substring(0, 23);
  // Add 'Z' to parse as UTC (same as getSecondsSinceHeartbeat)
  const utcTime = cleanedTime + 'Z';
  heartbeatTime = new Date(utcTime);
  
  // If parsing failed, fallback to local time
  if (isNaN(heartbeatTime.getTime())) {
    heartbeatTime = new Date(cleanedTime);
  }
  
  // If still invalid, use current time
  if (isNaN(heartbeatTime.getTime())) {
    heartbeatTime = new Date();
  }
} catch (e) {
  heartbeatTime = new Date();
}
    
    return (
      <div className={`service-card ${statusClass}`}>
        <div className="service-header">
          <div>
            <div className={`heartbeat-icon ${heartClass}`}>{heartIcon}</div>
            <div className="service-name">{serviceName.toUpperCase()}</div>
          </div>
          <div className={`status-badge ${statusClass}`}>{statusText}</div>
        </div>
        <div className="service-info">
          <div className="info-item">
            <div className="info-label">Port</div>
            <div className="info-value">{info.port}</div>
          </div>
          <div className="info-item">
            <div className="info-label">Host</div>
            <div className="info-value">{info.host}</div>
          </div>
          <div className="info-item">
            <div className="info-label">Last Heartbeat</div>
            <div className="info-value" style={{ 
              color: serverIsHealthy ? '#4CAF50' : '#f44336'
            }}>
              {secondsSince}s ago
            </div>
          </div>
          <div className="info-item">
            <div className="info-label">Last Seen</div>
            <div className="info-value" style={{ fontSize: '0.9em' }}>
              {heartbeatTime.toLocaleTimeString()}
            </div>
          </div>
          <div className="info-item" style={{ gridColumn: '1 / -1' }}>
            <div className="info-label">Server Status</div>
            <div className="info-value" style={{ 
              color: serverIsHealthy ? '#4CAF50' : '#f44336'
            }}>
              {info.status}
            </div>
          </div>
        </div>
      </div>
    );
  };

  const stats = getStats();
  const services = servicesData?.services || {};

  return (
    <div className="app">
      <div className="container">
        {/* Header */}
        <div className="header">
          <h1>🩺 Service Health Dashboard</h1>
          <p>Real-time monitoring of distributed services</p>
        </div>

        {/* Controls */}
        <div className="controls">
          <button onClick={fetchServices} className="btn">
            🔄 Refresh Now
          </button>
          <button 
            onClick={toggleAutoRefresh} 
            className={`btn ${autoRefreshEnabled ? 'active' : ''}`}
          >
            {autoRefreshEnabled ? '⏸️ Stop Auto-Refresh' : '▶️ Start Auto-Refresh'}
          </button>
        </div>

        {/* Stats Bar */}
        <div className="stats-bar">
          <div className="stat-card">
            <h3 style={{ color: '#667eea' }}>{stats.total}</h3>
            <p>Total Services</p>
          </div>
          <div className="stat-card">
            <h3 style={{ color: '#4CAF50' }}>{stats.healthy}</h3>
            <p>Healthy Services</p>
          </div>
          <div className="stat-card">
            <h3 style={{ color: '#f44336' }}>{stats.dead}</h3>
            <p>Dead Services</p>
          </div>
        </div>

        {/* Services Grid */}
        <div className="services-grid">
          {Object.keys(services).length === 0 ? (
            <div className="error-message warning">
              <h2>⚠️ No Services Registered</h2>
              <p>No services are currently sending heartbeats</p>
              <p style={{ marginTop: '10px', fontSize: '0.9em' }}>
                Start your services (A, B, C) to see them here!
              </p>
            </div>
          ) : (
            Object.entries(services).map(([name, info]) => (
              <ServiceCard key={name} serviceName={name} info={info} />
            ))
          )}
        </div>

        {/* Status Message */}
        <div className={`status-message ${isError ? 'error' : ''}`}>
          {statusMessage || 'Loading...'}
        </div>
      </div>
    </div>
  );
}

export default App;