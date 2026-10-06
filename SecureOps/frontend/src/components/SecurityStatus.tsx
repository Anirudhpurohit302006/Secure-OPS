import { Flame, Lock, ShieldCheck, Terminal } from 'lucide-react';
import StatusBadge from './StatusBadge';
import { boolStatus } from '../utils/status';

interface Props {
  firewallActive: boolean | null; selinuxMode: string | null; sshActive: boolean | null; sshPort?: number | null;
}

/** The four security controls shown on Home, Systems and Security. */
export default function SecurityStatus({ firewallActive, selinuxMode, sshActive, sshPort }: Props) {
  return (
    <ul className="control-list">
      <li><Flame size={15} aria-hidden="true" /><span>Firewall</span><StatusBadge status={boolStatus(firewallActive)} size="sm" /></li>
      <li><ShieldCheck size={15} aria-hidden="true" /><span>SELinux</span><StatusBadge status={selinuxMode} size="sm" /></li>
      <li><Terminal size={15} aria-hidden="true" /><span>SSH</span><StatusBadge status={boolStatus(sshActive)} size="sm" /></li>
      {sshPort != null && <li><Lock size={15} aria-hidden="true" /><span>SSH port</span><code>{sshPort}</code></li>}
    </ul>
  );
}
