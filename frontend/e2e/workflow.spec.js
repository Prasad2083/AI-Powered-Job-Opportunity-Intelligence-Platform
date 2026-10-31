import {test,expect} from '@playwright/test';

test('login, preview own resume, and log out with mocked API',async({page})=>{
  await page.route('**/api/**',async route=>{
    const path=new URL(route.request().url()).pathname;
    let data=[];
    if(path==='/api/auth/login') data={token:'browser-test-token',username:'alice',role:'USER'};
    if(path==='/api/resumes') data=[{id:1,filename:'cv.txt',active:true}];
    if(path==='/api/resumes/1/text') data={id:1,text:'Senior Quality Engineer with Java and Selenium'};
    await route.fulfill({json:data});
  });
  await page.goto('/');
  await page.getByRole('link',{name:'Log in',exact:true}).first().click();
  await page.getByPlaceholder('Username').fill('alice');
  await page.getByPlaceholder('Password').fill('test-password-123');
  await page.getByRole('button',{name:'Login',exact:true}).click();
  await expect(page.getByRole('heading',{name:'Jobs',exact:true})).toBeVisible();
  await page.getByRole('button',{name:'Resumes',exact:true}).click();
  await expect(page.getByText('cv.txt')).toBeVisible();
  await page.getByRole('button',{name:'Preview / edit text'}).click();
  await expect(page.getByRole('textbox',{name:'Resume text'})).toHaveValue('Senior Quality Engineer with Java and Selenium');
  await page.getByRole('button',{name:'Logout'}).click();
  await expect(page.getByRole('heading',{level:1})).toContainText('Your experience.');
  await page.getByRole('link',{name:'Log in',exact:true}).first().click();
  await page.getByPlaceholder('Username').fill('alice');
  await page.getByPlaceholder('Password').fill('test-password-123');
  await page.getByRole('button',{name:'Login',exact:true}).click();
  await expect(page.getByRole('heading',{name:'Jobs',exact:true})).toBeVisible();
  expect(await page.evaluate(()=>localStorage.getItem('token'))).toBe('browser-test-token');
});
