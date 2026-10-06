import { Link } from 'react-router-dom';
import PageHeader from '../components/PageHeader';
import SystemCard from '../components/SystemCard';
import { Async, EmptyState } from '../components/states';
import { useTargets } from '../hooks/TargetContext';
import { useApi } from '../hooks/useApi';
import { api } from '../services/api';

export default function SystemsPage() {
  const { selected } = useTargets();
  const systems = useApi(() => api.systems(), [], 60_000);
  return (
    <>
      <PageHeader title="Systems" subtitle="Monitored RHEL machines" />
      <Async state={systems}>
        {(all) => {
          const list = selected === 'all' ? all : all.filter((s) => String(s.id) === selected);
          if (list.length === 0) {
            return <EmptyState title="No systems configured" hint="Add a target to start monitoring.">
              <Link className="btn btn-primary" to="/settings">Open Settings</Link></EmptyState>;
          }
          return <div className="grid cols-2">{list.map((s) => <SystemCard key={s.id} system={s} />)}</div>;
        }}
      </Async>
    </>
  );
}
