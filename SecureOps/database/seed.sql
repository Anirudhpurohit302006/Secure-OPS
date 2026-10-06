-- Seeds the two RHEL targets. Idempotent. ssh_username 'CHANGE_ME' must be replaced
-- (edit it here before running, or fix it later under Settings -> Edit).
INSERT INTO machines (machine_name, hostname, ip_address, ssh_username) VALUES
  ('RHEL-01', 'rhel01', '192.168.159.129', 'CHANGE_ME'),
  ('RHEL-02', 'rhel02', '192.168.159.128', 'CHANGE_ME')
ON CONFLICT (machine_name) DO NOTHING;
