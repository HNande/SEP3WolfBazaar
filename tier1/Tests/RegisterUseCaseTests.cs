using System.ComponentModel.DataAnnotations;
using System.Net;
using System.Text;
using System.Text.Json;
using BlazorApp.DTO;
using BlazorApp.Services;
using Xunit;
using Assert = Xunit.Assert;

namespace BlazorApp.Tests;

public class RegisterUseCaseTests
{
    private static IList<ValidationResult> ValidateModel(object model)
    {
        var results = new List<ValidationResult>();
        var context = new ValidationContext(model);
        Validator.TryValidateObject(model, context, results, validateAllProperties: true);
        return results;
    }

    [Fact]
    public void BaseSequence_ValidDto_PassesClientValidation()
    {
        var dto = new CreateUserDto
        {
            FirstName = "John",
            LastName = "Doe",
            MemberName = "johndoe",
            MemberEmail = "john.doe@via.dk",
            Password = "Password123"
        };

        var errors = ValidateModel(dto);
        Assert.Empty(errors);
    }
    

    [Xunit.Theory]
    [InlineData("", "Doe", "user", "a@b.dk", "12345678")] // ALT1: Missing FirstName
    [InlineData("John", "", "user", "a@b.dk", "12345678")] // ALT1: Missing LastName
    [InlineData("John", "Doe", "", "a@b.dk", "12345678")] // ALT1: Missing Username
    [InlineData("John", "Doe", "user", "", "12345678")]    // ALT1: Missing Email
    [InlineData("John", "Doe", "user", "a@b.dk", "")]      // ALT1: Missing Password
    public void Alt1_MissingFields_FailsValidation(
        string first, string last, string username, string email, string password)
    {
        var dto = new CreateUserDto
        {
            FirstName = first,
            LastName = last,
            MemberName = username,
            MemberEmail = email,
            Password = password
        };

        var errors = ValidateModel(dto);
        Assert.NotEmpty(errors);
    }

    [Fact]
    public void Alt2_ExceedingMaxLengths_FailsValidation()
    {
        var dto = new CreateUserDto
        {
            FirstName = new string('a', 51),
            LastName = new string('b', 51),
            MemberName = new string('c', 51),
            MemberEmail = new string('d', 95) + "@via.dk", // 102 chars > 100
            Password = "1234567" // 7 chars < 8
        };

        var errors = ValidateModel(dto);
        Assert.Equal(5, errors.Count);
    }

    [Xunit.Theory]
    [InlineData("notanemail")]
    [InlineData("user@domain")]
    [InlineData("@domain.com")]
    public void Alt2_InvalidEmailFormat_FailsValidation(string badEmail)
    {
        var dto = new CreateUserDto
        {
            FirstName = "John",
            LastName = "Doe",
            MemberName = "johndoe",
            MemberEmail = badEmail,
            Password = "Password123"
        };

        var errors = ValidateModel(dto);
        Assert.Contains(errors, e => e.MemberNames.Contains(nameof(CreateUserDto.MemberEmail)));
    }

    [Fact]
    public async Task HttpUserService_RegisterAsync_SendsAllFieldsAndReturnsUser()
    {
        string? capturedBody = null;

        var handler = new FakeHttpMessageHandler(async request =>
        {
            Assert.Equal(HttpMethod.Post, request.Method);
            Assert.EndsWith("api/member", request.RequestUri!.ToString());

            capturedBody = await request.Content!.ReadAsStringAsync();

            var responseJson = JsonSerializer.Serialize(new
            {
                firstName = "John",
                lastName = "Doe",
                username = "johndoe"
            });

            return new HttpResponseMessage(HttpStatusCode.Created)
            {
                Content = new StringContent(responseJson, Encoding.UTF8, "application/json")
            };
        });

        var httpClient = new HttpClient(handler) { BaseAddress = new Uri("http://localhost:9009/") };
        var service = new HttpUserService(httpClient);

        var result = await service.RegisterAsync(new CreateUserDto
        {
            FirstName = " John ",
            LastName = " Doe ",
            MemberName = " johndoe ",
            MemberEmail = " john@via.dk ",
            Password = "Password123"
        });

        Assert.NotNull(capturedBody);
        Assert.Contains("\"firstName\":\"John\"", capturedBody);
        Assert.Contains("\"lastName\":\"Doe\"", capturedBody);
        Assert.Contains("\"username\":\"johndoe\"", capturedBody);
        Assert.Contains("\"email\":\"john@via.dk\"", capturedBody);
        Assert.Equal("johndoe", result.MemberName);
        Assert.Equal("john@via.dk", result.MemberEmail);
    }

    [Fact]
    public async Task HttpUserService_RegisterAsync_ThrowsMessageOnConflict()
    {
        var handler = new FakeHttpMessageHandler(_ =>
        {
            var errorJson = JsonSerializer.Serialize(new { status = 409, message = "Username is already taken" });
            return Task.FromResult(new HttpResponseMessage(HttpStatusCode.Conflict)
            {
                Content = new StringContent(errorJson, Encoding.UTF8, "application/json")
            });
        });

        var httpClient = new HttpClient(handler) { BaseAddress = new Uri("http://localhost:9009/") };
        var service = new HttpUserService(httpClient);

        var ex = await Assert.ThrowsAsync<Exception>(() => service.RegisterAsync(new CreateUserDto
        {
            FirstName = "John",
            LastName = "Doe",
            MemberName = "existingUser",
            MemberEmail = "john@via.dk",
            Password = "Password123"
        }));

        Assert.Equal("Username is already taken", ex.Message);
    }

    private sealed class FakeHttpMessageHandler : HttpMessageHandler
    {
        private readonly Func<HttpRequestMessage, Task<HttpResponseMessage>> _handler;

        public FakeHttpMessageHandler(Func<HttpRequestMessage, Task<HttpResponseMessage>> handler)
            => _handler = handler;

        protected override Task<HttpResponseMessage> SendAsync(
            HttpRequestMessage request, CancellationToken cancellationToken)
            => _handler(request);
    }
}