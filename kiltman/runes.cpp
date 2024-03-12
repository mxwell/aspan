#include "runes.h"

#include <stdexcept>

namespace NKiltMan {

void StringToRunes(const std::string& s, TRunes& result) {
    result.clear();
    for (size_t i = 0; i < s.size(); ) {
        uint8_t ch0 = s.at(i);
        if (ch0 < 0x80) {
            result.push_back(ch0);
            ++i;
        } else {
            if (i + 1 > s.size()) {
                throw std::runtime_error("Invalid UTF-8");
            }
            uint8_t ch1 = s.at(i + 1);
            if (ch1 == 0) {
                throw std::runtime_error("Invalid zero byte");
            }
            result.push_back((ch1 << 8) | ch0);
            i += 2;
        }
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