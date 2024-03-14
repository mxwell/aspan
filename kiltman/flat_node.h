#pragma once

#include <cstdint>
#include <limits>
#include <utility>
#include <vector>

namespace NKiltMan {

#define COMBO_RUNE(combo) (combo >> 24)
#define COMBO_NODE(combo) (combo & 0x00FFFFFF)
#define MAKE_COMBO(rune, node) ((rune << 24) | node)

struct FlatNode {
    using TKey = uint16_t;
    using TRuneId = uint8_t;
    using TNodeId = uint32_t;
    using TRuneNodeCombo = uint32_t;
    using TChildren = std::vector<TRuneNodeCombo>;

    static constexpr TKey kNoKey = std::numeric_limits<TKey>::max();
    static constexpr TNodeId kNoChild = std::numeric_limits<TNodeId>::max();

    TKey keyIndex;
    TChildren children;

    TNodeId FindChild(TRuneId ch) const {
        uint32_t n = children.size();
        uint32_t ch32 = ch;
        if (n <= 4) {
            for (const auto& combo : children) {
                if (COMBO_RUNE(combo) == ch32) {
                    return COMBO_NODE(combo);
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
        return keyIndex != kNoKey;
    }

    uint32_t GetSpace() const {
        return sizeof(children[0]) * children.capacity();
    }
};

#undef COMBO_RUNE
#undef COMBO_NODE

}  // namespace NKiltMan
