#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace NKiltMan {

using TRuneValue = uint16_t;
using TRunes = std::vector<TRuneValue>;

void StringToRunes(const std::string& s, TRunes& result);
void RunesToString(const TRunes& runes, std::string& result);

}  // namespace NKiltMan