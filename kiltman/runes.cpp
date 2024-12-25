#include "runes.h"

#include <stdexcept>
#include <unordered_map>

namespace NKiltMan {

using TLowercaseMapping = std::unordered_map<TRuneValue, TRuneValue>;

TLowercaseMapping GenerateLowercaseMapping() {
    TLowercaseMapping result = {
        {37072, 45264},
        {37328, 45520},
        {37584, 45776},
        {37840, 46032},
        {38096, 46288},
        {38352, 46544},
        {33232, 37329},
        {38608, 46800},
        {38864, 47056},
        {39120, 47312},
        {39376, 47568},
        {39632, 47824},
        {39888, 48080},
        {40144, 48336},
        {40400, 48592},
        {40656, 48848},
        {40912, 49104},
        {41168, 32977},
        {41424, 33233},
        {41680, 33489},
        {41936, 33745},
        {42192, 34001},
        {42448, 34257},
        {42704, 34513},
        {42960, 34769},
        {43216, 35025},
        {43472, 35281},
        {43728, 35537},
        {43984, 35793},
        {44240, 36049},
        {44496, 36305},
        {44752, 36561},
        {45008, 36817},
        {39123, 39379},
        {34512, 38609},
        {41682, 41938},
        {37586, 37842},
        {44754, 45010},
        {45266, 45522},
        {39634, 39890},
        {43219, 43475},
        {47826, 48082},
    };
    return result;
}

TRuneValue GetLowercase(TRuneValue rune) {
    static auto mapping = GenerateLowercaseMapping();
    auto it = mapping.find(rune);
    if (it != mapping.end()) {
        return it->second;
    }
    return rune;
}

ConversionResult StringToRunesNoExcept(const std::string& s, TRunes& result) {
    result.clear();
    for (size_t i = 0; i < s.size(); ) {
        uint8_t ch0 = s.at(i);
        if (ch0 < 0x80) {
            result.push_back(ch0);
            ++i;
        } else {
            if (i + 1 >= s.size()) {
                return ConversionResult::INVALID_UTF8;
            }
            uint8_t ch1 = s.at(i + 1);
            if (ch1 == 0) {
                return ConversionResult::UNEXPECTED_ZERO_BYTE;
            }
            auto rune = GetLowercase((ch1 << 8) | ch0);
            result.push_back(rune);
            i += 2;
        }
    }
    return ConversionResult::SUCCESS;
}

void StringToRunes(const std::string& s, TRunes& result) {
    auto conversionResult = StringToRunesNoExcept(s, result);
    switch (conversionResult) {
    case ConversionResult::SUCCESS:
        return;
    case ConversionResult::INVALID_UTF8:
        throw std::runtime_error("Invalid UTF-8");
    case ConversionResult::UNEXPECTED_ZERO_BYTE:
        throw std::runtime_error("Invalid zero byte");
    }
}

void RunesToString(const TRunes& runes, std::string& result) {
    result.clear();
    for (auto rune : runes) {
        uint8_t ch0 = rune & 0xFF;
        uint8_t ch1 = rune >> 8;
        if (ch1 == 0) {
            result.push_back(ch0);
        } else {
            result.push_back(ch0);
            result.push_back(ch1);
        }
    }
}

}  // namespace NKiltMan