#pragma once

#include <cstdint>
#include <limits>

namespace NKiltMan {

#define COMBO_RUNE(combo) (combo >> 24)
#define COMBO_NODE(combo) (combo & 0x00FFFFFF)
#define MAKE_COMBO(rune, node) ((rune << 24) | node)

#define COMBO_SUGGESTIONS_COUNT(combo) (combo >> 25)
#define COMBO_SUGGESTIONS_START(combo) (combo & 0x01FFFFFF)
#define MAKE_SUGG_COMBO(cnt, start) ((cnt << 25) | start)

struct FlatNode {
    using TKey = uint16_t;
    using TTransitionId = uint8_t;
    using TTerminalId = uint32_t;
    using TRuneId = uint8_t;
    using TNodeId = uint32_t;
    using TRuneNodeCombo = uint32_t;
    using TChildrenSize = uint8_t;
    using TChildrenStart = uint32_t;
    using TValueId = uint32_t;
    using TSuggestionsCount = uint8_t;
    using TSuggestionsOffset = uint32_t;
    using TSuggestionsComboPtr = uint32_t;

    static constexpr TKey kNoKey = std::numeric_limits<TKey>::max();
    static constexpr TTransitionId kNoTransitionId = std::numeric_limits<TTransitionId>::max();
    static constexpr TTerminalId kNoTerminalId = std::numeric_limits<TTerminalId>::max();
    static constexpr TNodeId kNoChild = std::numeric_limits<TNodeId>::max();

    TTerminalId terminalId;
    TChildrenSize childrenCount;
    TChildrenStart childrenStart;
    TSuggestionsComboPtr suggestionsPtr;

    TNodeId FindChild(TRuneId ch, const TRuneNodeCombo* childData) const {
        uint32_t n = childrenCount;
        uint32_t ch32 = ch;
        const TRuneNodeCombo* children = childData + childrenStart;
        if (n <= 4) {
            for (uint32_t i = 0; i < n; ++i) {
                if (COMBO_RUNE(children[i]) == ch32) {
                    return COMBO_NODE(children[i]);
                }
            }
        } else {
            uint32_t lf = 0;
            uint32_t rg = n - 1;
            while (lf + 1 < rg) {
                uint32_t mid = (lf + rg) / 2;
                auto runeId = COMBO_RUNE(children[mid]);
                if (runeId < ch32) {
                    lf = mid;
                } else {
                    rg = mid;
                }
            }
            for (uint32_t i = lf; i <= rg; ++i) {
                auto runeId = COMBO_RUNE(children[i]);
                if (runeId == ch32) {
                    return COMBO_NODE(children[i]);
                }
            }
        }
        return kNoChild;
    }

    bool IsTerminal() const {
        return terminalId != kNoTerminalId;
    }

    uint32_t GetSpace() const {
        return 0;
    }
};

#undef COMBO_RUNE
#undef COMBO_NODE

}  // namespace NKiltMan
