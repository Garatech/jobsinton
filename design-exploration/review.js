(() => {
const $=s=>document.querySelector(s);
$('#load-comparison').addEventListener('click',()=>{
 const screen=$('#screen-choice').value;
 $('#left-frame').src=$('#left-choice').value+'/index.html#'+screen;
 $('#right-frame').src=$('#right-choice').value+'/index.html#'+screen;
 $('#comparison-grid').classList.toggle('mobile',$('#width-choice').value==='mobile');
 $('#comparison-grid').hidden=false;
});
$('#decision-form').addEventListener('submit',e=>{
 e.preventDefault();
 const lines=['JobSinton — Design review','',...Array.from(new FormData(e.target),([key,value])=>key+': '+(value||'Undecided'))];
 const url=URL.createObjectURL(new Blob([lines.join('\n')+'\n'],{type:'text/plain;charset=utf-8'}));
 const a=document.createElement('a');a.href=url;a.download='jobsinton-design-review.txt';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
 $('#export-status').textContent='Review notes downloaded.';
});
})();