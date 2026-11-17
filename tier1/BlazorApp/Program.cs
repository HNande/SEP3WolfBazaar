using BlazorApp.Auth;
using BlazorApp.Components;
using BlazorApp.Services;
using Microsoft.AspNetCore.Components.Authorization;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddRazorComponents()
    .AddInteractiveServerComponents();

// for tests without server
builder.Services.AddSingleton<IUserService, LocalUserService>();
builder.Services.AddSingleton<IItemService, LocalItemService>();

// for normal run
//builder.Services.AddHttpClient<IUserService, HttpUserService>(client =>
//{
//    client.BaseAddress = new Uri("http://localhost:8080/");
//});

//builder.Services.AddHttpClient<IItemService, HttpItemService>(client =>
//{
//    client.BaseAddress = new Uri("http://localhost:8080/");
//});

builder.Services.AddScoped<AuthenticationStateProvider, AuthProvider>();
builder.Services.AddAuthorizationCore();

var app = builder.Build();

if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Error", createScopeForErrors: true);
    app.UseHsts();
}

app.UseStatusCodePagesWithReExecute("/not-found",
    createScopeForStatusCodePages: true);
app.UseHttpsRedirection();

app.UseAntiforgery();

app.MapStaticAssets();
app.MapRazorComponents<App>()
    .AddInteractiveServerRenderMode();

app.Run();