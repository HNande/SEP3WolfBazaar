using System.Text.Json.Serialization;

namespace BlazorApp.DTO;

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum Category
{
    [JsonStringEnumMemberName("UNSPECIFIED")]
    CategoryUnspecified = 0,
    [JsonStringEnumMemberName("CLOTHING")]
    CategoryClothing = 1,
    [JsonStringEnumMemberName("FOOTWEAR")]
    CategoryFootwear = 2,
    [JsonStringEnumMemberName("ELECTRONICS")]
    CategoryElectronics = 3,
    [JsonStringEnumMemberName("MAKEUP")]
    CategoryMakeup = 4,
    [JsonStringEnumMemberName("JEWELRY")]
    CategoryJewelry = 5,
    [JsonStringEnumMemberName("HOUSEHOLD_GOODS")]
    CategoryHouseholdGoods = 6,
    [JsonStringEnumMemberName("PRIVATE_SERVICES")] CategoryPrivateServices = 7,
    [JsonStringEnumMemberName("CURIOSITIES")]
    CategoryCuriosities = 8,
    [JsonStringEnumMemberName("PET_SUPPLIES")]
    CategoryPetSupplies = 9,
    [JsonStringEnumMemberName("GARDENING")]
    CategoryGardening = 10,
    [JsonStringEnumMemberName("OUTDOOR_GOODS")]
    CategoryOutdoorGoods = 11
}