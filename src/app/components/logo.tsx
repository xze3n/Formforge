import svgPaths from "../../imports/svg-y589gepwu";

export function Logo() {
  return (
    <div className="h-[56px] relative shrink-0 w-[140px] scale-75 origin-left">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        {/* Text - FormForge */}
        <div className="absolute content-stretch flex flex-col h-[35px] items-start left-[47.5px] top-[13px] w-[152px]">
          <div className="flex-[1_0_0] min-h-px min-w-px relative w-[203.475px]">
            <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
              <p className="absolute font-['Inter:Bold',sans-serif] font-bold leading-[0] left-0 not-italic text-[#9810fa] text-[30px] top-[-1.6px] tracking-[-0.75px] whitespace-nowrap">
                <span className="leading-[36px]">Form</span>
                <span className="leading-[36px] text-[#ad46ff]">
                  Forge
                </span>
              </p>
            </div>
          </div>
        </div>

        {/* Icon */}
        <div className="absolute content-stretch flex flex-col items-start left-0 size-[48px] top-[4px]">
          <div className="h-[48px] overflow-clip relative shrink-0 w-full">
            {/* Document shape */}
            <div className="absolute bottom-[12.5%] left-1/4 right-[20.83%] top-[12.5%]">
              <svg
                className="absolute block size-full"
                fill="none"
                preserveAspectRatio="none"
                viewBox="0 0 26 36"
              >
                <path d={svgPaths.p30c757f0} fill="#A855F7" />
              </svg>
            </div>

            {/* Folded corner */}
            <div className="absolute inset-[12.5%_20.83%_66.67%_58.33%]">
              <svg
                className="absolute block size-full"
                fill="none"
                preserveAspectRatio="none"
                viewBox="0 0 10 10"
              >
                <path d={svgPaths.p27b4f300} fill="#7C3AED" />
              </svg>
            </div>

            {/* Line 1 */}
            <div className="absolute inset-[45.83%_33.33%_54.17%_37.5%]">
              <div className="absolute inset-[-1px_-7.14%]">
                <svg
                  className="block size-full"
                  fill="none"
                  preserveAspectRatio="none"
                  viewBox="0 0 16 2"
                >
                  <path
                    d="M1 1H15"
                    stroke="white"
                    strokeLinecap="round"
                    strokeWidth={2}
                  />
                </svg>
              </div>
            </div>

            {/* Line 2 */}
            <div className="absolute inset-[58.33%_33.33%_41.67%_37.5%]">
              <div className="absolute inset-[-1px_-7.14%]">
                <svg
                  className="block size-full"
                  fill="none"
                  preserveAspectRatio="none"
                  viewBox="0 0 16 2"
                >
                  <path
                    d="M1 1H15"
                    stroke="white"
                    strokeLinecap="round"
                    strokeWidth={2}
                  />
                </svg>
              </div>
            </div>

            {/* Line 3 */}
            <div className="absolute inset-[70.83%_45.83%_29.17%_37.5%]">
              <div className="absolute inset-[-1px_-12.5%]">
                <svg
                  className="block size-full"
                  fill="none"
                  preserveAspectRatio="none"
                  viewBox="0 0 10 2"
                >
                  <path
                    d="M1 1H9"
                    stroke="white"
                    strokeLinecap="round"
                    strokeWidth={2}
                  />
                </svg>
              </div>
            </div>

            {/* Yellow circle */}
            <div className="absolute inset-[62.5%_10.96%_8.33%_59.87%]">
              <svg
                className="absolute block size-full"
                fill="none"
                preserveAspectRatio="none"
                viewBox="0 0 14 14"
              >
                <path d={svgPaths.p3296bc80} fill="#FCD34D" />
              </svg>
            </div>

            {/* Yellow star */}
            <div className="absolute inset-[66.67%_15.13%_12.5%_64.04%]">
              <svg
                className="absolute block size-full"
                fill="none"
                preserveAspectRatio="none"
                viewBox="0 0 10 10"
              >
                <path d={svgPaths.p6022440} fill="#F59E0B" />
              </svg>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}