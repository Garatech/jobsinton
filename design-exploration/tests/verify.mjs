import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import { createRequire } from 'node:module';
import { pathToFileURL } from 'node:url';
const require=createRequire(path.resolve('frontend/package.json'));
const {chromium,expect}=require('@playwright/test');
const root=path.resolve('design-exploration');
const out=path.resolve('design-preview-evidence');
await fs.mkdir(out,{recursive:true});
const browser=await chromium.launch();
const variants=['00-current','01-folio','02-ledger','03-atlas','04-nocturne'];
const report=[];
const scriptReference=await fs.readFile(path.join(root,variants[0],'app.js'),'utf8');
for(const variant of variants){
 assert.equal(await fs.readFile(path.join(root,variant,'app.js'),'utf8'),scriptReference,'Preview behavior must remain equivalent across variants');
}
const errors=[];
let activePage;
async function buttonContrast(locator){
 await locator.hover();
 const pair=await locator.evaluate(e=>{const s=getComputedStyle(e);return [s.color,s.backgroundColor];});
 const lum=css=>{const values=css.match(/[\d.]+/g).slice(0,3).map(Number).map(n=>n/255).map(n=>n<=.04045?n/12.92:((n+.055)/1.055)**2.4);return values[0]*.2126+values[1]*.7152+values[2]*.0722;};
 const a=lum(pair[0]),b=lum(pair[1]),ratio=(Math.max(a,b)+.05)/(Math.min(a,b)+.05);
 assert.ok(ratio>=4.5,'Hovered action contrast '+ratio.toFixed(2)+' '+pair.join(' / '));
}
async function fit(page,label){
 await page.evaluate(()=>document.fonts.ready);
 const dims=await page.evaluate(()=>({width:innerWidth,scroll:document.documentElement.scrollWidth}));
 if(dims.scroll>dims.width+1){
  const outside=await page.evaluate(()=>[...document.querySelectorAll('body *')].filter(e=>{const r=e.getBoundingClientRect();return r.width&&r.right>innerWidth+1;}).slice(0,15).map(e=>({tag:e.tagName,id:e.id,cls:e.className,width:e.getBoundingClientRect().width,right:e.getBoundingClientRect().right})));
  console.log('Overflow detail',JSON.stringify(outside));
 }
 assert.ok(dims.scroll<=dims.width+1,label+' horizontal overflow: '+JSON.stringify(dims));
}
try {
 for(const variant of variants){
  for(const width of [320,390,768,1440]){
   console.log('Checking',variant,width);
   const page=await browser.newPage({viewport:{width,height:960}});
   activePage=page;
   const localErrors=[];
   page.on('pageerror',e=>localErrors.push(e.message));
   page.on('request',r=>{if(/^https?:/.test(r.url()))errors.push('Unexpected network request: '+r.url());});
   const url=pathToFileURL(path.join(root,variant,'index.html')).href;
   await page.goto(url+'#discover');
   await page.waitForSelector('.job');
   await expect(page.locator('.job')).toHaveCount(6);
   await fit(page,variant+' catalog '+width);
   await page.screenshot({path:path.join(out,variant+'-catalog-'+width+'.png'),fullPage:true});
   await page.locator('#search').fill('java');
   await expect(page.locator('.job')).toHaveCount(4);
   await page.locator('#filter-mode').selectOption('Remote');
   await expect(page.locator('.job')).toHaveCount(2);
   await page.locator('#filter-role').selectOption('SAP');
   await expect(page.locator('.job')).toHaveCount(0);
   await page.locator('#empty-reset').click();
   await expect(page.locator('.job')).toHaveCount(6);
   await page.keyboard.press('Tab');
   await page.locator('[data-open="northline"]').focus();
   const outline=await page.locator('[data-open="northline"]').evaluate(e=>getComputedStyle(e).outlineStyle);
   assert.notEqual(outline,'none','Job title must retain a focus indicator');
   await page.keyboard.press('Enter');
   await expect(page.locator('#detail')).toHaveJSProperty('open',true);
   await fit(page,variant+' detail '+width);
   await page.screenshot({path:path.join(out,variant+'-detail-'+width+'.png'),fullPage:true});
   await buttonContrast(page.locator('#detail-save'));
   await page.locator('#detail-save').click();
   await expect(page.locator('#detail-save')).toHaveAttribute('aria-pressed','true');
   await page.keyboard.press('Escape');
   await expect(page.locator('#detail')).toHaveJSProperty('open',false);
   await expect(page.locator('[data-open="northline"]')).toBeFocused();
   await page.locator('[data-view="saved"]').click();
   await expect(page.locator('.job')).toHaveCount(1);
   await page.locator('[data-save="northline"]').click();
   await expect(page.locator('.job')).toHaveCount(0);
   await page.locator('[data-view="discover"]').click();
   await expect(page.locator('.job')).toHaveCount(6);
   await page.locator('[data-save="northline"]').click();
   await page.locator('[data-view="saved"]').click();
   await expect(page.locator('.job')).toHaveCount(1);
   await page.locator('[data-open="northline"]').click();
   await page.locator('#detail-save').click();
   await page.keyboard.press('Escape');
   await expect(page.locator('#view-heading')).toBeFocused();
   await expect(page.locator('.job')).toHaveCount(0);
   await page.locator('[data-view="profile"]').click();
   if(variant==='00-current'){
    await expect(page.locator('#current-hero')).toBeVisible();
    await expect(page.locator('body')).toHaveCSS('background-color','rgb(245, 245, 239)');
    await expect(page.locator('#hero-title')).toHaveCSS('font-family',/Segoe UI/);
   }
   await buttonContrast(page.locator('#profile-form button[type="submit"]'));
   await page.locator('#fill-example').click();
   await page.locator('#profile-workmode').selectOption('Hybrid');
   await expect(page.locator('#days-field')).toBeVisible();
   await page.locator('#profile-days').selectOption('2');
   await page.locator('#profile-form button[type="submit"]').click();
   await expect(page.locator('#profile-message')).toHaveText(/Profile saved/);
   await expect(page.locator('#profile-summary-content')).toHaveText(/Madrid, Spain/);
   await page.locator('#profile-city').fill('Valencia');
   await expect(page.locator('#profile-summary-content')).toHaveText(/Madrid, Spain/);
   await page.locator('#profile-workmode').selectOption('Remote');
   await expect(page.locator('#days-field')).toBeHidden();
   await page.locator('#profile-workmode').selectOption('Hybrid');
   await expect(page.locator('#profile-days')).toHaveValue('');
   await page.locator('#profile-form button[type="submit"]').click();
   await expect(page.locator('#form-error')).toHaveText(/required/);
   await page.locator('#profile-workmode').selectOption('Remote');
   await page.locator('#technology').fill('java');
   await page.locator('#add-technology').click();
   await expect(page.locator('#technology-error')).toHaveText(/already added/);
   await page.locator('#technology').fill('X'.repeat(40));
   await page.locator('#add-technology').click();
   await page.locator('#profile-city').fill('C'.repeat(80));
   await page.locator('#profile-form button[type="submit"]').click();
   await fit(page,variant+' profile long text '+width);
   await page.locator('#profile-city').fill('Madrid');
   await page.locator('#profile-form button[type="submit"]').click();
   await page.screenshot({path:path.join(out,variant+'-profile-'+width+'.png'),fullPage:true});
   page.once('dialog',d=>d.accept());
   await page.locator('#clear-profile').click();
   await expect(page.locator('#profile-summary-content')).toHaveText(/No saved profile/);
   await page.reload();
   await expect(page.locator('#profile-role')).toHaveValue('');
   assert.equal(localErrors.length,0,localErrors.join('\n'));
   report.push({variant,width,result:'passed'});
   await page.close();
  }
 }
 const portal=await browser.newPage({viewport:{width:1440,height:960}});
 activePage=portal;
 await portal.goto(pathToFileURL(path.join(root,'index.html')).href);
 await fit(portal,'Review portal desktop');
 await portal.locator('#load-comparison').click();
 await portal.frameLocator('#left-frame').locator('#job-list .job').first().waitFor();
 await portal.frameLocator('#right-frame').locator('#job-list .job').first().waitFor();
 await portal.locator('#decision-0').selectOption('Folio');
 const downloaded=portal.waitForEvent('download');
 await portal.locator('#decision-form button').click();
 const download=await downloaded;
 assert.equal(download.suggestedFilename(),'jobsinton-design-review.txt');
 await portal.setViewportSize({width:320,height:960});
 await fit(portal,'Review portal mobile');
 await portal.screenshot({path:path.join(out,'review-portal-mobile.png'),fullPage:true});
 assert.equal(errors.length,0,errors.join('\n'));
 const result={status:'passed',sourceCommit:process.env.GITHUB_SHA||'local',transport:'file://',browser:'Chromium',cases:report,portal:'passed'};
 await fs.writeFile(path.join(out,'verification.json'),JSON.stringify(result,null,2));
 await fs.writeFile(path.join(root,'VERIFICATION_RESULT.json'),JSON.stringify(result,null,2));
 await fs.appendFile(path.join(root,'VERIFICATION.md'),'\n## Packaged result\n\nPassed: 20 variant/viewport journeys and the review portal in Chromium using file URLs. Source commit: '+result.sourceCommit+'. See VERIFICATION_RESULT.json.\n');
 console.log(JSON.stringify({status:'passed',variantViewportCases:report.length,portal:'passed'}));
} catch(error){
 if(activePage&&!activePage.isClosed())await activePage.screenshot({path:path.join(out,'failure.png'),fullPage:true}).catch(()=>{});
 await fs.writeFile(path.join(out,'failure.txt'),String(error.stack||error));
 throw error;
} finally { await browser.close(); }
