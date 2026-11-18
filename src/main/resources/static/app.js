const $=s=>document.querySelector(s);let logs=[];
const modal=$('#modal');$('#newLog').onclick=()=>{modal.classList.add('show');$('#formResult').textContent=''};$('#close').onclick=()=>modal.classList.remove('show');modal.addEventListener('click',e=>{if(e.target===modal)modal.classList.remove('show')});
function esc(v=''){return String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}
function short(v,n=14){return v&&v.length>n?v.slice(0,n)+'…':(v||'—')}
async function load(){try{const [health,items]=await Promise.all([fetch('/api/health').then(r=>r.json()),fetch('/api/logs?limit=100').then(r=>{if(!r.ok)throw Error('Supabase query failed');return r.json()})]);logs=items||[];$('#total').textContent=logs.length;$('#anchored').textContent=logs.filter(x=>x.transaction_id).length;$('#verified').textContent=logs.filter(x=>x.integrity_valid===true).length;const a=health.algorand||{};$('#networkStatus').textContent=a.demoMode?'DEMO MODE':(a.connected?'ONLINE':'OFFLINE');$('#networkStatus').style.color=a.demoMode?'#f5bd65':(a.connected?'var(--cyan)':'#ff9eae');$('#round').textContent=a.demoMode?'Simulated · no chain calls':(a.connected?'Node reachable':'Check node configuration');$('#lastRound').textContent=a.demoMode?'Not applicable':(a.lastRound??'—');$('#account').textContent=a.demoMode?'Demo placeholder':(a.accountConfigured?'Configured':'Not configured');$('#writeMode').textContent=a.demoMode?'Demo only':(a.writesEnabled?'Enabled':'Disabled');render()}catch(e){$('#logRows').innerHTML=`<tr><td colspan="5" class="empty">${esc(e.message)}. Configure Supabase and restart the Java server.</td></tr>`;$('#networkStatus').textContent='ERROR'}}
function render(){if(!logs.length){$('#logRows').innerHTML='<tr><td colspan="5" class="empty">No security events yet. Record your first event to create an audit trail.</td></tr>';return}$('#logRows').innerHTML=logs.map(x=>`<tr><td><span class="event-name">${esc(x.event_type)}</span><span class="event-source">${esc(x.source)} · ${esc(x.actor||'system')}</span></td><td><span class="pill ${esc(x.severity)}">${esc(x.severity)}</span></td><td>${esc(new Date(x.created_at).toLocaleString())}</td><td><span class="chain-proof">${x.transaction_id?short(x.transaction_id,13):esc(x.chain_status||'PENDING')}</span></td><td>${x.transaction_id?`<button class="verify" data-id="${esc(x.id)}">Verify ↗</button>`:'<span class="verify">—</span>'}</td></tr>`).join('');document.querySelectorAll('.verify[data-id]').forEach(b=>b.onclick=()=>verify(b.dataset.id))}
async function verify(id){const b=document.querySelector(`.verify[data-id="${CSS.escape(id)}"]`);if(b)b.textContent='Checking…';try{const r=await fetch('/api/verify/'+encodeURIComponent(id),{method:'POST'});const d=await r.json();alert(d.status==='VERIFIED'?'Integrity verified: database hash matches the Algorand transaction.':(d.status==='DEMO_VERIFIED'?'Demo verification passed: the stored SHA-256 hash matches the saved log. No Algorand transaction was submitted.':'Not verified: transaction not found or hash mismatch.\nDatabase hash match: '+d.databaseHashMatches+'\nBlockchain match: '+d.blockchainMatch)); }catch(e){alert('Verification request failed: '+e.message)}finally{load()}}
$('#logForm').addEventListener('submit',async e=>{e.preventDefault();const f=new FormData(e.target);const body=Object.fromEntries(f.entries());$('#formResult').textContent='Hashing and storing…';try{const r=await fetch('/api/logs',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});const d=await r.json();if(!r.ok)throw Error(d.error||'Request failed');$('#formResult').textContent='Stored · SHA-256 '+d.sha256+' · '+d.chainStatus+(d.transactionId?' · Tx '+d.transactionId:'');e.target.reset();await load()}catch(err){$('#formResult').textContent='Error: '+err.message}});
// Refresh dashboard with visible feedback
async function refreshDashboard(button) {
    const originalText = button.textContent;

    button.disabled = true;
    button.textContent = '↻ Refreshing...';

    let status = document.getElementById('refreshStatus');

    if (!status) {
        status = document.createElement('span');
        status.id = 'refreshStatus';
        status.style.cssText =
            'color:#56e0d0;font-size:10px;margin-left:10px;';
        button.insertAdjacentElement('afterend', status);
    }

    status.textContent = 'Loading latest data...';

    try {
        await load();
        status.textContent = 'Refresh completed';
    } catch (error) {
        status.textContent = 'Refresh failed: ' + error.message;
        console.error(error);
    } finally {
        button.disabled = false;
        button.textContent = originalText;

        setTimeout(() => {
            status.textContent = '';
        }, 4000);
    }
}

$('#refresh').addEventListener('click', function () {
    refreshDashboard(this);
});

$('#refresh2').addEventListener('click', function () {
    refreshDashboard(this);
});

// Initial dashboard loading
load();

// Automatic refresh every 30 seconds
setInterval(load, 30000);
